package com.example.timedauctions.service.impl;

// --- Necessary Imports ---
import com.example.timedauctions.client.ProductServiceClient; // Assuming Feign client exists
import com.example.timedauctions.client.UserServiceClient;   // Assuming Feign client exists
import com.example.timedauctions.client.dto.CategoryDto;
import com.example.timedauctions.client.dto.ProductDto;
import com.example.timedauctions.client.dto.UserBasicInfoDto;
import com.example.timedauctions.commands.AuctionLifecycleCommands; // Create this package/classes
import com.example.timedauctions.config.AuctionTimingProperties;
import com.example.timedauctions.config.RabbitMqConfig;
import com.example.timedauctions.dto.*;
import com.example.timedauctions.entity.*;
import com.example.timedauctions.exception.*; // Create custom exceptions
import com.example.timedauctions.mapper.TimedAuctionMapper;
import com.example.timedauctions.repository.AuctionCommentRepository;
import com.example.timedauctions.repository.AuctionProxyBidRepository; // Add later
import com.example.timedauctions.repository.BidRepository;
import com.example.timedauctions.repository.TimedAuctionRepository;
import com.example.timedauctions.service.TimedAuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient; // Add later for locking
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class TimedAuctionServiceImpl implements TimedAuctionService {

    private final TimedAuctionRepository timedAuctionRepository;
    private final BidRepository bidRepository;
    // private final AuctionProxyBidRepository auctionProxyBidRepository; // Inject when needed
    // private final AuctionCommentRepository auctionCommentRepository; // Inject when needed

    private final ProductServiceClient productServiceClient; // Assuming setup via @EnableFeignClients
    private final UserServiceClient userServiceClient;       // Assuming setup
    private final AuctionProxyBidRepository auctionProxyBidRepository;
    private final AuctionCommentRepository auctionCommentRepository;
    private final RedissonClient redissonClient;

    private final RabbitTemplate rabbitTemplate;
    private final TimedAuctionMapper auctionMapper;
    private final AuctionTimingProperties timingProperties;
    // private final RedissonClient redissonClient; // Inject when needed


    @Override
    @Transactional
    public TimedAuctionDetailsDto createAuction(String sellerId, CreateTimedAuctionDto createDto) {
        log.info("Attempting to create timed auction for product ID: {} by seller: {}", createDto.getProductId(), sellerId);

        // 1. Fetch Product & Seller Details (Blocking calls - consider async later if needed)
        ProductDto product = fetchProductDetails(createDto.getProductId());
        UserBasicInfoDto sellerInfo = fetchUserDetails(sellerId); // Fetch full user info if needed, or just basic

        // 2. Determine Timing & Status
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveStartTime = createDto.getStartTime() != null ? createDto.getStartTime() : now;
        // Calculate end time based on duration in days
        LocalDateTime endTime = createDto.getEndTime();
        AuctionStatus initialStatus = effectiveStartTime.isAfter(now) ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE;

        // Prevent scheduling in the past if startTime was provided but already passed
        if (initialStatus == AuctionStatus.SCHEDULED && effectiveStartTime.isBefore(now)) {
            log.warn("Provided start time {} for product {} is in the past. Starting auction now.", effectiveStartTime, product.getId());
            effectiveStartTime = now;
            // NOTE: endTime remains as provided by the user, the duration just shrinks if start is pulled forward.
            initialStatus = AuctionStatus.ACTIVE;
        }

        // 3. Calculate Initial Increment (based on start price)
        BigDecimal initialIncrement = getIncrement(createDto.getStartPrice());

        // 4. Snapshot Category IDs
        Set<Long> categoryIdsSnapshot = product.getCategories() == null
                ? Set.of()
                : product.getCategories().stream()
                .map(CategoryDto::getId)
                .collect(Collectors.toSet());

        // 5. Build TimedAuction Entity
        TimedAuction auction = TimedAuction.builder()
                .productId(product.getId())
                .sellerId(sellerId)
                .productTitleSnapshot(product.getTitle())
                .productImageUrlSnapshot(product.getImageUrls() != null && !product.getImageUrls().isEmpty() ? product.getImageUrls().get(0) : null)
                .sellerUsernameSnapshot(sellerInfo.getUsername()) // Assuming UserBasicInfoDto has username
                .productCategoryIdsSnapshot(categoryIdsSnapshot)
                .startPrice(createDto.getStartPrice())
                .reservePrice(createDto.getReservePrice())
                .currentBid(null) // No bids initially
                .highestBidderId(null)
                .highestBidderUsernameSnapshot(null)
                .currentBidIncrement(initialIncrement) // Set initial increment
                .startTime(effectiveStartTime)
                .endTime(endTime)
                .status(initialStatus)
                .reserveMet(false)
                // .softCloseEnabled(true) // Set based on global config or future DTO field
                .build();

        // 6. Save the Auction Entity
        TimedAuction savedAuction = timedAuctionRepository.save(auction);
        log.info("Timed Auction entity saved with ID: {} and status: {}", savedAuction.getId(), savedAuction.getStatus());

        // 7. Schedule Start/End via RabbitMQ Delayed Messages
        if (savedAuction.getStatus() == AuctionStatus.SCHEDULED) {
            scheduleAuctionStart(savedAuction);
        } else if (savedAuction.getStatus() == AuctionStatus.ACTIVE) {
            // Auction starts immediately, schedule the end
            scheduleAuctionEnd(savedAuction);
            // Potentially publish an 'AuctionStarted' internal event
            publishInternalEvent(savedAuction, "STARTED");
        }

        // 8. Map to Details DTO and Return
        // Initial state: no bids, calculate time left, next bid is start price
        long timeLeftMs = calculateTimeLeftMs(savedAuction);
        BigDecimal nextBid = savedAuction.getStartPrice();

        return auctionMapper.mapToTimedAuctionDetailsDto(
                savedAuction,
                product,
                Collections.emptyList(), // No bids yet
                timeLeftMs,
                nextBid
        );
    }


    @Override
    @Transactional(readOnly = true)
    public TimedAuctionDetailsDto getAuctionDetails(UUID auctionId) {
        log.debug("Fetching details for timed auction: {}", auctionId);
        TimedAuction auction = timedAuctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Timed auction not found: " + auctionId));

        // 1. Fetch Product Details (Handle Not Found Gracefully)
        ProductDto productDto = null;
        try {
            productDto = fetchProductDetails(auction.getProductId());
        } catch (ProductNotFoundException e) {
            log.warn("Product details not found for product ID {} (auction {}).", auction.getProductId(), auctionId);
        } catch (Exception e) {
            log.error("Error fetching product details for auction {}.", auctionId, e);
        }

        // 2. Fetch Recent Bids (Visible Bids)
        // Fetch last ~20 visible bids for display
        Pageable bidPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "bidTime"));
        Page<Bid> recentBidsPage = bidRepository.findByTimedAuctionId(auctionId, bidPageable);
        List<BidDto> recentBidDtos = auctionMapper.mapToBidDtoList(recentBidsPage.getContent());

        // 3. Calculate Dynamic State
        long timeLeftMs = calculateTimeLeftMs(auction);
        BigDecimal nextBidAmount = calculateNextBidAmount(auction);

        // 4. Map to Details DTO
        return auctionMapper.mapToTimedAuctionDetailsDto(
                auction,
                productDto,
                recentBidDtos,
                timeLeftMs,
                nextBidAmount
        );
    }

    @Override
    @Transactional(readOnly = true) // Good practice for read operations
    public Page<TimedAuctionSummaryDto> getActiveAuctions(Pageable pageable) {
        log.debug("Service fetching ACTIVE timed auctions page: {}", pageable);
        // Fetch ACTIVE auctions using the repository method
        Page<TimedAuction> auctionPage = timedAuctionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);

        // Map the Page<TimedAuction> to Page<TimedAuctionSummaryDto> using the mapper
        // The .map function on Page handles the conversion for each element
        return auctionPage.map(auctionMapper::mapToTimedAuctionSummaryDto);
    }


    @Override
    // Remove @Transactional here, handle it in the internal method
    public void placeMaxBid(UUID auctionId, String bidderId, PlaceMaxBidDto bidDto) {
        log.info("Processing max bid for auction {} from bidder {} with max {}",
                auctionId, bidderId, bidDto.getMaxBid());

        // Use Redisson distributed lock to prevent race conditions on this specific auction
        RLock lock = redissonClient.getLock("timed_auction_lock:" + auctionId.toString());
        boolean lockAcquired = false;
        try {
            // Try to acquire lock for 10 seconds, lease time 30 seconds (adjust as needed)
            lockAcquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!lockAcquired) {
                log.warn("Could not acquire lock for auction {} to place bid", auctionId);
                throw new IllegalStateException("Could not process bid at this time, please try again shortly.");
            }

            // Fetch basic auction info first (read-only, outside main transaction potentially)
            TimedAuction auction = timedAuctionRepository.findById(auctionId)
                    .orElseThrow(() -> new AuctionNotFoundException("Timed auction not found: " + auctionId));

            // --- Initial Validations ---
            validateAuctionStateForBidding(auction); // Checks ACTIVE status and not ended
            validateNotSeller(auction, bidderId); // Checks bidder is not seller
            if (bidDto.getMaxBid() == null || bidDto.getMaxBid().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBidException("Max bid amount must be positive.");
            }
            // Optional: Check if max bid is at least the required next bid amount
            BigDecimal requiredNext = calculateNextBidAmount(auction);
            if (requiredNext != null && bidDto.getMaxBid().compareTo(requiredNext) < 0) {
                throw new InvalidBidException("Your maximum bid must be at least the next required bid amount: " + requiredNext);
            }


            // Fetch bidder username *once*
            UserBasicInfoDto bidderInfo = fetchUserDetails(bidderId);

            // --- Call Core Proxy Bid Handling Logic ---
            // This internal method will handle its own transaction
            boolean stateChanged = handleNewMaxBid(auction, bidderId, bidderInfo.getUsername(), bidDto.getMaxBid());

            // Optional: If state changed, maybe trigger soft close rescheduling check here?
            // Soft close logic needs careful placement - should it be inside handleNewMaxBid? Yes.

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Bid processing interrupted for auction {}", auctionId, e);
            throw new IllegalStateException("Bid processing was interrupted.");
        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released lock for auction {}", auctionId);
            }
        }
    }

    // --- Core Proxy Bidding Logic (New Private Method) ---
    @Transactional // This core logic needs to be atomic
    // propagation = Propagation.REQUIRES_NEW // Optional: Consider if a new transaction is better
    boolean handleNewMaxBid(TimedAuction auction, String bidderId, String bidderUsername, BigDecimal newMaxBid) {
        log.debug("Handling new max bid logic for auction {}, bidder {}, max {}", auction.getId(), bidderId, newMaxBid);

        // --- Track original state ---
        BigDecimal originalVisibleBid = auction.getCurrentBid();
        String originalLeaderId = auction.getHighestBidderId();
        LocalDateTime originalEndTime = auction.getEndTime(); // For soft-close check

        // --- 1. Find/Update Current Bidder's Proxy Bid ---
        AuctionProxyBid currentProxy = auctionProxyBidRepository
                .findByTimedAuctionIdAndBidderId(auction.getId(), bidderId)
                .orElse(null);

        // Don't allow lowering max bid (common rule, prevents gaming)
        if (currentProxy != null && newMaxBid.compareTo(currentProxy.getMaxBid()) < 0) {
            throw new InvalidBidException("You cannot lower your maximum bid.");
        }

        if (currentProxy == null) {
            currentProxy = AuctionProxyBid.builder()
                    .timedAuctionId(auction.getId())
                    .bidderId(bidderId)
                    .maxBid(newMaxBid)
                    // submissionTime will be set by @UpdateTimestamp or manually set now
                    .submissionTime(LocalDateTime.now())
                    .build();
            log.info("Creating new proxy bid for bidder {} on auction {}", bidderId, auction.getId());
        } else {
            // Only update if the new max bid is higher
            if (newMaxBid.compareTo(currentProxy.getMaxBid()) > 0) {
                log.info("Updating proxy bid for bidder {} on auction {} from {} to {}",
                        bidderId, auction.getId(), currentProxy.getMaxBid(), newMaxBid);
                currentProxy.setMaxBid(newMaxBid);
                currentProxy.setSubmissionTime(LocalDateTime.now()); // Manually update time if not using @UpdateTimestamp effectively
            } else {
                log.info("New max bid {} is not higher than existing {} for bidder {}. No update needed.",
                        newMaxBid, currentProxy.getMaxBid(), bidderId);
                // If max bid isn't higher, no recalculation is needed.
                return false; // Indicate no state change occurred
            }
        }
        auctionProxyBidRepository.save(currentProxy); // Save new or updated proxy

        // --- 2. Get All Proxies and Determine Winner/Runner-Up ---
        List<AuctionProxyBid> allProxies = auctionProxyBidRepository
                .findByTimedAuctionIdOrderByMaxBidDescSubmissionTimeAsc(auction.getId());

        if (allProxies.isEmpty()) {
            // Should not happen if we just saved one, but defensive check
            log.error("No proxy bids found for auction {} after saving one!", auction.getId());
            return false;
        }

        AuctionProxyBid winnerProxy = allProxies.get(0);
        AuctionProxyBid runnerUpProxy = allProxies.size() > 1 ? allProxies.get(1) : null;

        // --- 3. Calculate New Visible Price ---
        BigDecimal newVisiblePrice;
        BigDecimal startPrice = auction.getStartPrice();
        BigDecimal currentVisibleBid = auction.getCurrentBid() == null ? BigDecimal.ZERO : auction.getCurrentBid(); // Use 0 if no bids yet

        if (runnerUpProxy == null) {
            // Only one bidder (the winner)
            // Visible price is the start price, but cannot exceed winner's max bid.
            // Ensure it's at least start price.
            newVisiblePrice = startPrice.max(currentVisibleBid); // Should start at startPrice
            newVisiblePrice = newVisiblePrice.min(winnerProxy.getMaxBid()); // Clamp at winner's max

        } else {
            // Two or more bidders
            BigDecimal runnerUpMax = runnerUpProxy.getMaxBid();
            BigDecimal increment = getIncrement(runnerUpMax); // Increment based on RUNNER-UP's max
            BigDecimal requiredToBeatRunnerUp = runnerUpMax.add(increment);

            // Floor price must be at least start price AND beat runner-up
            BigDecimal floorPrice = requiredToBeatRunnerUp.max(startPrice);

            newVisiblePrice = floorPrice.min(winnerProxy.getMaxBid()); // Final price is the floor, clamped by winner's max
        }

        // --- 4. Check for Change ---
        // Check if winner ID changed OR visible price increased
        boolean winnerChanged = !winnerProxy.getBidderId().equals(originalLeaderId);
        boolean priceIncreased = originalVisibleBid == null || newVisiblePrice.compareTo(originalVisibleBid) > 0;
        boolean stateChanged = winnerChanged || priceIncreased;

        log.debug("Auction {}: Original Leader={}, Original Bid={}, New Leader={}, New Visible Bid={}, StateChanged={}",
                auction.getId(), originalLeaderId, originalVisibleBid, winnerProxy.getBidderId(), newVisiblePrice, stateChanged);

        // --- 5. If State Changed, Update Auction & Record Bid ---
        if (stateChanged) {
            log.info("Auction {} state changed. New Leader: {}, New Visible Bid: {}",
                    auction.getId(), winnerProxy.getBidderId(), newVisiblePrice);

            // Fetch winner's username (needed for snapshot)
            // TODO: Optimize - can we get username snapshot from AuctionProxyBid entity? No, fetch it.
            String winnerUsername = fetchUserDetails(winnerProxy.getBidderId()).getUsername();

            // Create a new *visible* Bid record
            // Determine if this specific visible bid update was directly caused by a manual action
            // For simplicity now, mark all system-generated bids as auto=true
            Bid visibleBid = Bid.builder()
                    .timedAuctionId(auction.getId())
                    .bidderId(winnerProxy.getBidderId())
                    .bidderUsernameSnapshot(winnerUsername)
                    .amount(newVisiblePrice)
                    .isAutoBid(true) // Mark as system-generated for now
                    .bidTime(LocalDateTime.now()) // Timestamp of this calculation
                    .build();
            bidRepository.save(visibleBid);
            log.debug("Saved new visible bid record for auction {}", auction.getId());

            // Update the main Auction entity
            auction.setCurrentBid(newVisiblePrice);
            auction.setHighestBidderId(winnerProxy.getBidderId());
            auction.setHighestBidderUsernameSnapshot(winnerUsername);
            auction.setCurrentBidIncrement(getIncrement(newVisiblePrice)); // Increment needed for NEXT bid
            boolean reserveNowMet = auction.getReservePrice() != null && newVisiblePrice.compareTo(auction.getReservePrice()) >= 0;
            if (!auction.isReserveMet() && reserveNowMet) {
                log.info("Reserve price met for auction {}", auction.getId());
            }
            auction.setReserveMet(reserveNowMet);


            // --- Check Soft-Close ---
            // Trigger soft-close only if:
            // a) This is the first bid ever placed (originalLeaderId == null) OR
            // b) The LEADER has changed (winnerChanged == true)
            // AND the bid was placed within the threshold period.
            boolean isFirstBid = originalLeaderId == null && priceIncreased; // First visible bid placed
            boolean shouldTriggerSoftClose = isFirstBid || winnerChanged;

            if (shouldTriggerSoftClose && timingProperties.getSoftClose().isEnabled()) {
                long thresholdMillis = timingProperties.getSoftClose().getThresholdMinutes() * 60 * 1000L;
                long millisLeft = Duration.between(LocalDateTime.now(), originalEndTime).toMillis();

                if (millisLeft > 0 && millisLeft <= thresholdMillis) {
                    LocalDateTime newEndTime = LocalDateTime.now().plusMinutes(timingProperties.getSoftClose().getExtensionMinutes());
                    // Ensure extension doesn't shorten the auction if threshold is large
                    if (newEndTime.isAfter(originalEndTime)) {
                        auction.setEndTime(newEndTime);
                        log.info("Soft-close triggered for auction {}. New end time: {}", auction.getId(), newEndTime);
                        // Reschedule the end task via RabbitMQ
                        scheduleAuctionEnd(auction); // Reschedule with new end time
                    }
                }
            }

            // Save the updated auction state
            timedAuctionRepository.save(auction);
            log.debug("Updated auction {} state in DB.", auction.getId());

            // Optional: Publish internal event
            publishInternalEvent(auction, "BID_PLACED");

            return true; // Indicate state changed

        } else {
            log.info("No change in leader or visible price for auction {}. New max bid from {} did not change outcome yet.",
                    auction.getId(), bidderId);
            return false; // Indicate no state change occurred
        }
    }

    // --- Helper Methods ---

    private ProductDto fetchProductDetails(Long productId) {
        try {
            log.debug("Fetching product details for ID: {}", productId);
            return productServiceClient.getProductById(productId); // Replace with actual Feign client method
        } catch (Exception e) {
            log.error("Failed to fetch product details for ID: {}", productId, e);
            throw new ProductNotFoundException("Product not found with ID: " + productId);
        }
    }

    private UserBasicInfoDto fetchUserDetails(String userId) {
        try {
            log.debug("Fetching username for user ID: {}", userId);
            // Assuming Feign client returns a Map<String, UserBasicInfoDto>
            Map<String, UserBasicInfoDto> users = userServiceClient.getUsersBasicInfoByIds(Collections.singletonList(userId)); // Replace with actual method
            UserBasicInfoDto userInfo = users.get(userId);
            if (userInfo == null) {
                throw new UserNotFoundException("User info not found for ID: " + userId);
            }
            return userInfo;
        } catch (Exception e) {
            log.error("Failed to fetch user info for ID: {}", userId, e);
            // Wrap or rethrow appropriate exception
            throw new UserNotFoundException("User info retrieval failed for ID: " + userId);
        }
    }

    private void scheduleAuctionStart(TimedAuction auction) {
        long delayMillis = Duration.between(LocalDateTime.now(), auction.getStartTime()).toMillis();
        if (delayMillis > 0) {
            log.info("Scheduling start for auction {} in {} ms", auction.getId(), delayMillis);
            AuctionLifecycleCommands.StartAuctionCommand command = new AuctionLifecycleCommands.StartAuctionCommand(auction.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.TD_AUCTION_SCHEDULE_EXCHANGE, // Use timed auction exchange
                    RabbitMqConfig.TD_START_ROUTING_KEY,       // Use timed auction routing key
                    command,
                    message -> {
                        message.getMessageProperties().setHeader("x-delay", (int) Math.min(delayMillis, Integer.MAX_VALUE));
                        return message;
                    }
            );
        } else {
            log.warn("Attempted to schedule start for auction {} but delay was not positive.", auction.getId());
            // Consider immediate start or error handling
        }
    }

    private void scheduleAuctionEnd(TimedAuction auction) {
        long delayMillis = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
        if (delayMillis > 0) {
            log.info("Scheduling end for auction {} in {} ms", auction.getId(), delayMillis);
            // Ensure AuctionLifecycleCommands exist or create equivalents
            AuctionLifecycleCommands.EndAuctionCommand command = new AuctionLifecycleCommands.EndAuctionCommand(auction.getId());
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.TD_AUCTION_SCHEDULE_EXCHANGE, // Use timed auction exchange
                    RabbitMqConfig.TD_END_ROUTING_KEY,         // Use timed auction routing key
                    command,
                    message -> {
                        // RabbitMQ delayed message plugin uses integer for header
                        int delayHeaderValue = (int) Math.min(delayMillis, Integer.MAX_VALUE);
                        // Handle potential overflow for very long delays if necessary, although 49 days is usually enough
                        if (delayMillis > Integer.MAX_VALUE) {
                            log.warn("Auction {} end delay ({}) exceeds max RabbitMQ delay. Clamping to MAX_VALUE.", auction.getId(), delayMillis);
                        }
                        message.getMessageProperties().setHeader("x-delay", delayHeaderValue);
                        return message;
                    }
            );
        } else {
            log.warn("Attempted to schedule end for auction {} but delay was not positive. It might end immediately.", auction.getId());
            // Potentially trigger end logic directly or via non-delayed message
        }
    }

    // Placeholder for internal event publishing (e.g., to different queues for background tasks)
    private void publishInternalEvent(TimedAuction auction, String eventType) {
        log.debug("Placeholder: Publishing internal event '{}' for auction {}", eventType, auction.getId());
        // rabbitTemplate.convertAndSend(RabbitMqConfig.TD_AUCTION_EVENTS_EXCHANGE, "td.auction.event." + eventType.toLowerCase(), eventPayload);
    }


    private long calculateTimeLeftMs(TimedAuction auction) {
        if (auction.getStatus() == AuctionStatus.ACTIVE && auction.getEndTime() != null) {
            long timeLeft = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
            return Math.max(0, timeLeft); // Ensure non-negative
        }
        return 0;
    }

    // Calculates the minimum amount required for the *next* valid manual bid
    private BigDecimal calculateNextBidAmount(TimedAuction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            return null; // Or some indicator that bidding isn't possible
        }
        // If no bids yet, the next required bid is the start price
        if (auction.getCurrentBid() == null || auction.getHighestBidderId() == null) {
            return auction.getStartPrice();
        }
        // Otherwise, it's the current visible bid plus the increment *at that level*
        BigDecimal currentVisibleBid = auction.getCurrentBid();
        BigDecimal increment = getIncrement(currentVisibleBid); // Use helper
        return currentVisibleBid.add(increment);
    }


    // --- BIDDING VALIDATION HELPERS ---
    private void validateAuctionStateForBidding(TimedAuction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new InvalidAuctionStateException("Auction is not active. Status: " + auction.getStatus());
        }
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            // This check might be slightly redundant if status is updated promptly, but good defense
            throw new InvalidAuctionStateException("Auction has already ended.");
        }
    }

    private void validateNotSeller(TimedAuction auction, String bidderId) {
        if (auction.getSellerId().equals(bidderId)) {
            throw new InvalidBidException("Seller cannot bid on their own auction.");
        }
    }


    // --- Increment Calculation Logic (Copy/Adapt from LiveAuction) ---
    private BigDecimal getIncrement(BigDecimal currentBid) {
        // Handle null or zero current bid (use start price logic)
        if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
            // For the very first bid, the required amount is startPrice,
            // but the increment calculation might still be based on tiers starting from 0 or startPrice.
            // Let's base it on tiers from 0 for simplicity, matching the proxy logic's use case.
            currentBid = BigDecimal.ZERO;
        }
        // Define your increment tiers (same as live auction or different?)
        // Example using VND tiers from previous context
        if (currentBid.compareTo(new BigDecimal("10000000")) >= 0) return new BigDecimal("2000000");
        if (currentBid.compareTo(new BigDecimal("5000000")) >= 0) return new BigDecimal("1000000");
        if (currentBid.compareTo(new BigDecimal("2000000")) >= 0) return new BigDecimal("500000");
        if (currentBid.compareTo(new BigDecimal("1000000")) >= 0) return new BigDecimal("200000");
        if (currentBid.compareTo(new BigDecimal("500000")) >= 0) return new BigDecimal("100000");
        if (currentBid.compareTo(new BigDecimal("200000")) >= 0) return new BigDecimal("50000");
        if (currentBid.compareTo(new BigDecimal("100000")) >= 0) return new BigDecimal("20000");
        if (currentBid.compareTo(new BigDecimal("50000")) >= 0) return new BigDecimal("10000");
        return new BigDecimal("5000"); // Default lowest increment
    }

    // --- TODO: Implement Proxy Bidding Logic ---
    // @Transactional // This internal method likely needs its own transaction
    // private void handleNewMaxBid(TimedAuction auction, String bidderId, String bidderUsername, BigDecimal maxBid) {
    //    // ... Lock, Fetch Proxies, Save/Update Proxy, Sort, Calc Price, Save Bid, Update Auction ...
    // }


    @Override
    public Page<TimedAuctionSummaryDto> getSellerAuctions(String sellerId, AuctionStatus status, Set<Long> categoryIds, LocalDateTime from, Pageable pageable) {
        log.warn("getSellerAuctions not implemented yet");
        return Page.empty(); // Placeholder
    }

    /**
     * Creates a new comment or reply on a timed auction.
     *
     * @param auctionId  The ID of the auction being commented on.
     * @param userId     The ID of the user posting the comment.
     * @param commentDto DTO containing comment text and optional parentId.
     * @return The created CommentDto.
     */
    @Override
    @Transactional
    public CommentDto createComment(UUID auctionId, String userId, CreateCommentDto commentDto) {
        log.info("User {} creating comment for auction {}", userId, auctionId);

        // 1. Validate Auction Exists (Optional but recommended)
        TimedAuction auction = timedAuctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Cannot post comment, auction not found: " + auctionId));
        // Optional: Check if auction is active/ended? Allow comments after end? For now, allow if exists.

        // 2. Validate Parent Comment Exists (if it's a reply)
        if (commentDto.getParentId() != null) {
            if (!auctionCommentRepository.existsById(commentDto.getParentId())) {
                throw new CommentNotFoundException("Cannot reply, parent comment not found: " + commentDto.getParentId()); // Create this exception
            }
            // Optional: Check if parent comment belongs to the same auctionId
        }

        // 3. Fetch User Details for Snapshot
        UserBasicInfoDto userInfo = fetchUserDetails(userId);

        // 4. Create and Save Comment Entity
        AuctionComment comment = AuctionComment.builder()
                .timedAuctionId(auctionId)
                .userId(userId)
                .usernameSnapshot(userInfo.getUsername())
                .commentText(commentDto.getCommentText())
                .parentId(commentDto.getParentId())
                // createdAt will be set by @CreationTimestamp
                .build();

        AuctionComment savedComment = auctionCommentRepository.save(comment);
        log.info("Saved comment with ID {} for auction {}", savedComment.getId(), auctionId);

        // 5. Map to DTO and Return (simple mapping, no replies needed for create response)
        return auctionMapper.mapToCommentDto(savedComment);
    }

    /**
     * Retrieves comments for a specific auction.
     * Currently fetches all comments and builds a nested structure one level deep.
     * Consider pagination for top-level comments if performance becomes an issue.
     *
     * @param auctionId The ID of the auction.
     * @return A list of top-level CommentDto objects, each potentially containing direct replies.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getComments(UUID auctionId) {
        log.debug("Fetching comments for auction {}", auctionId);

        // 1. Validate Auction Exists (Optional check)
        if (!timedAuctionRepository.existsById(auctionId)) {
            throw new AuctionNotFoundException("Cannot get comments, auction not found: " + auctionId);
        }

        // 2. Fetch ALL comments for the auction (Strategy 2: Fetch All, Build Tree)
        // Sort by creation time to maintain order when building tree
        List<AuctionComment> allComments = auctionCommentRepository.findByTimedAuctionIdOrderByCreatedAtAsc(auctionId); // Need this repo method

        // 3. Use Mapper to build the nested DTO structure
        List<CommentDto> nestedCommentDtos = auctionMapper.mapToNestedCommentDtoList(allComments);

        log.debug("Returning {} top-level comments for auction {}", nestedCommentDtos.size(), auctionId);
        return nestedCommentDtos;
    }


    // --- TODO: Create Commands Package ---
    // Create package com.example.timedauctions.commands
    // Create record StartAuctionCommand(UUID auctionId) {}
    // Create record EndAuctionCommand(UUID auctionId) {}
    // Create record CancelAuctionCommand(UUID auctionId, String sellerId) {} // if needed

    // --- TODO: Create Exceptions Package ---
    // Create package com.example.timedauctions.exception
    // Create AuctionNotFoundException extends RuntimeException
    // Create ProductNotFoundException extends RuntimeException
    // Create UserNotFoundException extends RuntimeException
    // Create InvalidAuctionStateException extends RuntimeException
    // Create InvalidBidException extends RuntimeException

    // --- TODO: Setup Feign Clients ---
    // Ensure Feign clients (ProductServiceClient, UserServiceClient) interfaces exist
    // and are configured with @FeignClient annotation pointing to correct service names.
    // Ensure client DTOs (ProductDto, UserBasicInfoDto, CategoryDto) exist.
    // Add @EnableFeignClients to the main application class.


}