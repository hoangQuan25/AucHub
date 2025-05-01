package com.example.liveauctions.service.impl;

import com.example.liveauctions.client.ProductServiceClient; // Feign Client for Products
import com.example.liveauctions.client.UserServiceClient; // Feign Client for Users
import com.example.liveauctions.client.dto.CategoryDto;
import com.example.liveauctions.client.dto.ProductDto; // DTO from Products service
import com.example.liveauctions.client.dto.UserBasicInfoDto; // DTO from Users service
import com.example.liveauctions.commands.AuctionLifecycleCommands;
import com.example.liveauctions.commands.AuctionLifecycleCommands.StartAuctionCommand; // Our command record
import com.example.liveauctions.config.AuctionTimingProperties;
import com.example.liveauctions.config.RabbitMqConfig; // Constants for RabbitMQ
import com.example.liveauctions.dto.*;
import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.Bid;
import com.example.liveauctions.entity.LiveAuction;
import com.example.liveauctions.exception.*;
import com.example.liveauctions.manager.AuctionLifecycleManager;
import com.example.liveauctions.mapper.AuctionMapper;
import com.example.liveauctions.repository.BidRepository;
import com.example.liveauctions.repository.LiveAuctionRepository;
import com.example.liveauctions.service.LiveAuctionService;
import com.example.liveauctions.service.WebSocketEventPublisher; // For WebSocket events
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveAuctionServiceImpl implements LiveAuctionService {

    private final LiveAuctionRepository liveAuctionRepository;
    private final ProductServiceClient productServiceClient; // Feign client
    private final UserServiceClient userServiceClient;     // Feign client
    private final RabbitTemplate rabbitTemplate;
    private final AuctionMapper auctionMapper; // Your MapStruct or similar mapper

    private final BidRepository bidRepository; // Add BidRepository dependency
    private final RedissonClient redissonClient; // Add RedissonClient dependency
    private final WebSocketEventPublisher webSocketEventPublisher; // For publishing events
    private final AuctionLifecycleManager lifecycleManager;
    private final AuctionTimingProperties timing;

    // Assume getIncrement is available, maybe in a helper class or static method
    // private final AuctionHelper auctionHelper;

    @Override
    @Transactional // Make the creation process transactional
    public LiveAuctionDetailsDto createAuction(String sellerId, CreateLiveAuctionDto createDto) {
        log.info("Attempting to create auction for product ID: {} by seller: {}", createDto.getProductId(), sellerId);

        // 1. Fetch Product Details via Feign
        ProductDto product = fetchProductDetails(createDto.getProductId());

        // 2. Fetch Seller Username via Feign
        String sellerUsername = fetchSellerUsername(sellerId);

        // 3. Determine Timing and Status
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = createDto.getStartTime() != null ? createDto.getStartTime() : now;
        LocalDateTime endTime = startTime.plusMinutes(createDto.getDurationMinutes());
        AuctionStatus initialStatus = startTime.isAfter(now) ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE;

        // Prevent scheduling in the past if startTime was provided but already passed
        if (initialStatus == AuctionStatus.SCHEDULED && startTime.isBefore(now)) {
            log.warn("Provided start time {} for product {} is in the past. Starting auction now.", startTime, product.getId());
            startTime = now; // Correct start time to now
            endTime = startTime.plusMinutes(createDto.getDurationMinutes()); // Recalculate end time
            initialStatus = AuctionStatus.ACTIVE;
        }


        // 4. Calculate Initial Increment
        BigDecimal initialIncrement = getIncrement(createDto.getStartPrice());

        // LiveAuctionServiceImpl#createAuction
        Set<Long> categoryIdsSnapshot = product.getCategories() == null
                ? Set.of()
                : product.getCategories().stream()
                .map(CategoryDto::getId)
                .collect(Collectors.toSet());



        // 5. Build LiveAuction Entity
        LiveAuction auction = LiveAuction.builder()
                .productId(product.getId())
                .sellerId(sellerId)
                .productTitleSnapshot(product.getTitle()) // Snapshot
                .productImageUrlSnapshot(product.getImageUrls() != null && !product.getImageUrls().isEmpty() ? product.getImageUrls().get(0) : null) // Snapshot main image
                .sellerUsernameSnapshot(sellerUsername) // Snapshot
                .startPrice(createDto.getStartPrice())
                .reservePrice(createDto.getReservePrice())
                .currentBid(null) // No bids initially
                .highestBidderId(null)
                .highestBidderUsernameSnapshot(null)
                .currentBidIncrement(initialIncrement) // Set initial increment
                .startTime(startTime)
                .endTime(endTime)
                .status(initialStatus)
                .reserveMet(false)
                .productCategoryIdsSnapshot(categoryIdsSnapshot)
                .build();

        // 6. Save the Auction Entity
        LiveAuction savedAuction = liveAuctionRepository.save(auction);
        log.info("Auction entity saved with ID: {} and status: {}", savedAuction.getId(), savedAuction.getStatus());

        // 7. Schedule Start or Handle Immediate Start
        if (savedAuction.getStatus() == AuctionStatus.SCHEDULED) {
            long delayMillis = Duration.between(now, savedAuction.getStartTime()).toMillis();
            if (delayMillis > 0) {
                StartAuctionCommand command = new StartAuctionCommand(savedAuction.getId());
                rabbitTemplate.convertAndSend(
                        // Use the DELAYED exchange here
                        RabbitMqConfig.AUCTION_SCHEDULE_EXCHANGE,
                        RabbitMqConfig.START_ROUTING_KEY,
                        command,
                        message -> {
                            // CORRECT WAY: Set the 'x-delay' header
                            // Clamp delay to Integer.MAX_VALUE as header value might be expected as Integer
                            int delayHeaderValue = (int) Math.min(delayMillis, Integer.MAX_VALUE);

                            // Set the header only if the delay is positive
                            if (delayHeaderValue > 0) {
                                message.getMessageProperties().setHeader("x-delay", delayHeaderValue);
                            }
                            return message;
                        }
                );
            } else {
                // Should not happen due to check above, but as fallback handle immediate start
                log.warn("Scheduled auction {} start time is not in the future, handling immediate start.", savedAuction.getId());
                startAuctionInternal(savedAuction); // Handle immediate start (schedules end, publishes event)
            }
        } else if (savedAuction.getStatus() == AuctionStatus.ACTIVE) {
            // Auction starts immediately
            log.info("Auction {} starting immediately.", savedAuction.getId());
            startAuctionInternal(savedAuction); // Handle immediate start (schedules end, publishes event)
        }

        // 8. Map to Details DTO and Return (enrichment happens in getAuctionDetails)
        // For the create response, we might return a simpler DTO or the full one.
        // Let's assume we can build the details DTO here *without* extra Feign calls for now,
        // using the data we already fetched/snapshotted.
        // The full enrichment happens when GET /details is called.
        return auctionMapper.mapToLiveAuctionDetailsDto(
                savedAuction,
                product, // Pass the fetched product
                Collections.emptyList(), // No bids yet
                Duration.between(LocalDateTime.now(), savedAuction.getEndTime()).toMillis(), // Initial time left
                savedAuction.getCurrentBidIncrement() == null ? savedAuction.getStartPrice() : savedAuction.getCurrentBidIncrement().add(savedAuction.getStartPrice()) // Initial next bid
        );
        // Alternative: Return simpler DTO with just ID and key info.
        // return auctionMapper.toLiveAuctionCreatedDto(savedAuction);
    }

    // --- Helper Methods ---

    private ProductDto fetchProductDetails(Long productId) {
        try {
            log.debug("Fetching product details for ID: {}", productId);
            // Direct Feign call (blocks the current thread)
            return productServiceClient.getProductById(productId);
        } catch (Exception e) { // Catch specific Feign exceptions ideally (using ErrorDecoder)
            log.error("Failed to fetch product details for ID: {}", productId, e);
            // Throw your custom exception
            throw new ProductNotFoundException("Product not found with ID: " + productId);
        }
    }

    private String fetchSellerUsername(String sellerId) {
        try {
            log.debug("Fetching username for seller ID: {}", sellerId);
            // Direct Feign call
            Map<String, UserBasicInfoDto> users = userServiceClient.getUsersBasicInfoByIds(Collections.singletonList(sellerId));
            UserBasicInfoDto sellerInfo = users.get(sellerId);
            if (sellerInfo == null) {
                throw new UserNotFoundException("Seller info not found for ID: " + sellerId);
            }
            return sellerInfo.getUsername();
        } catch (Exception e) {
            log.error("Failed to fetch seller username for ID: {}", sellerId, e);
            throw new UserNotFoundException("Seller info not found for ID: " + sellerId);
        }
    }

    // Placeholder for the logic needed when an auction becomes ACTIVE
    private void startAuctionInternal(LiveAuction auction) {
        log.info("Executing internal start logic for auction {}", auction.getId());
        // 1. Schedule the auction end using RabbitMQ delayed message
        lifecycleManager.scheduleAuctionEnd(auction); // Same logic as used in the start listener later

        // 2. Publish AuctionStartedEvent (or initial state update) to RabbitMQ for WebSocket broadcast
        publishAuctionStateUpdate(auction); // Helper method needed
    }

    // Placeholder: Publishes state update event via RabbitMQ
    private void publishAuctionStateUpdate(LiveAuction auction) {
        // ... Logic to build event DTO and publish to AUCTION_EVENTS_EXCHANGE ...
        log.info("Placeholder: publishAuctionStateUpdate called for auction {}", auction.getId());
    }

    // Placeholder for the getIncrement logic
    private BigDecimal getIncrement(BigDecimal currentBid) {
        // Handle null or zero current bid (first bid increment)
        if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
            // You might want a specific increment for the very first bid,
            // or just use the lowest tier. Let's use the lowest tier for now.
            currentBid = BigDecimal.ZERO;
        }
        // >= 10,000,000 VNĐ -> Increment 2,000,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("10000000")) >= 0) {
            return new BigDecimal("2000000");
        }
        // >= 5,000,000 VNĐ -> Increment 1,000,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("5000000")) >= 0) {
            return new BigDecimal("1000000");
        }
        // >= 2,000,000 VNĐ -> Increment 500,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("2000000")) >= 0) {
            return new BigDecimal("500000");
        }
        // >= 1,000,000 VNĐ -> Increment 200,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("1000000")) >= 0) {
            return new BigDecimal("200000");
        }
        // >= 500,000 VNĐ -> Increment 100,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("500000")) >= 0) {
            return new BigDecimal("100000");
        }
        // >= 200,000 VNĐ -> Increment 50,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("200000")) >= 0) {
            return new BigDecimal("50000");
        }
        // >= 100,000 VNĐ -> Increment 20,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("100000")) >= 0) {
            return new BigDecimal("20000");
        }
        // >= 50,000 VNĐ -> Increment 10,000 VNĐ
        if (currentBid.compareTo(new BigDecimal("50000")) >= 0) {
            return new BigDecimal("10000");
        }

        // Default lowest increment for bids < 50,000 VNĐ
        return new BigDecimal("5000");
    }

    // --- Implement other LiveAuctionService methods (getAuctionDetails, placeBid, getActiveAuctions) ---
    // ...
    @Override
    @Transactional(readOnly = true) // Read-only transaction is appropriate here
    public LiveAuctionDetailsDto getAuctionDetails(UUID auctionId) {
        log.debug("Fetching details for auction: {}", auctionId);
        LiveAuction auction = liveAuctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found: " + auctionId));

        // Fetch Product Details (Direct Blocking Call)
        ProductDto productDto = null;
        try {
            productDto = fetchProductDetails(auction.getProductId());
        } catch (ProductNotFoundException e) {
            log.warn("Product details not found for product ID {} (auction {}).", auction.getProductId(), auctionId);
            // Continue without product details
        } catch (Exception e) {
            log.error("Error fetching product details for auction {}.", auctionId, e);
            // Continue without product details
        }

        // Fetch Recent Bids (Blocking JPA call)
        Pageable bidPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "bidTime"));
        Page<Bid> recentBidsPage = bidRepository.findByLiveAuctionId(auctionId, bidPageable);
        List<BidDto> recentBidDtos = auctionMapper.mapToBidDtoList(recentBidsPage.getContent());

        // 4. Calculate Dynamic State (Time Left, Next Bid Amount)
        long timeLeftMs = 0;
        if (auction.getStatus() == AuctionStatus.ACTIVE && auction.getEndTime() != null) {
            timeLeftMs = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
            timeLeftMs = Math.max(0, timeLeftMs);
        }

        BigDecimal nextBidAmount = null;
        if (auction.getStatus() == AuctionStatus.ACTIVE) {
            BigDecimal currentBid = auction.getCurrentBid() == null ? BigDecimal.ZERO : auction.getCurrentBid();
            if (auction.getHighestBidderId() == null) {
                nextBidAmount = auction.getStartPrice();
            } else if (auction.getCurrentBidIncrement() != null) {
                nextBidAmount = currentBid.add(auction.getCurrentBidIncrement());
            } else {
                // Fallback needed? Should not happen if increment is always calculated
                log.warn("currentBidIncrement is null for active auction {} during details fetch.", auctionId);
                nextBidAmount = currentBid; // Default or error indicator?
            }
        }


        // 5. Map to Details DTO using Mapper
        // The mapper needs to handle combining LiveAuction, optional ProductDto, List<BidDto>, and calculated values
        return auctionMapper.mapToLiveAuctionDetailsDto(
                auction,
                productDto,
                recentBidDtos,
                timeLeftMs,
                nextBidAmount
        );
    }

    @Override
    @Transactional
    public void placeBid(UUID auctionId, String bidderId, PlaceBidDto bidDto) {

        String lockKey = "auction_lock:" + auctionId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean lockAcquired = false;

        try {
            lockAcquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!lockAcquired) {
                throw new IllegalStateException("Could not process bid, please retry.");
            }

            LiveAuction auction = liveAuctionRepository.findById(auctionId)
                    .orElseThrow(() -> new AuctionNotFoundException("Auction not found: " + auctionId));

            /* ---------- 1. Validations -------------------------------------------------- */
            if (auction.getStatus() != AuctionStatus.ACTIVE) {
                throw new InvalidAuctionStateException("Auction is not active.");
            }
            if (LocalDateTime.now().isAfter(auction.getEndTime())) {
                throw new InvalidAuctionStateException("Auction has already ended.");
            }
            if (auction.getSellerId().equals(bidderId)) {
                throw new InvalidBidException("Seller cannot bid on own auction.");
            }
            if (bidderId.equals(auction.getHighestBidderId())) {
                throw new InvalidBidException("You are already the highest bidder.");
            }

            BigDecimal currentBid = auction.getCurrentBid() == null ? BigDecimal.ZERO : auction.getCurrentBid();
            BigDecimal requiredAmount = (auction.getHighestBidderId() == null)
                    ? auction.getStartPrice()
                    : currentBid.add(auction.getCurrentBidIncrement());

            if (bidDto.getAmount().compareTo(requiredAmount) < 0) {
                throw new InvalidBidException("Bid too low. Minimum required: " + requiredAmount);
            }

            /* ---------- 2. Persist new bid --------------------------------------------- */
            String bidderUsername = fetchBidderUsername(bidderId);
            Bid newBid = bidRepository.save(Bid.builder()
                    .liveAuctionId(auctionId)
                    .bidderId(bidderId)
                    .bidderUsernameSnapshot(bidderUsername)
                    .amount(bidDto.getAmount())
                    .build());

            /* ---------- 3. Update auction financials ----------------------------------- */
            auction.setCurrentBid(bidDto.getAmount());
            auction.setHighestBidderId(bidderId);
            auction.setHighestBidderUsernameSnapshot(bidderUsername);
            auction.setCurrentBidIncrement(getIncrement(auction.getCurrentBid()));

            /* ---------- 4. Reserve-met & timing rules ---------------------------------- */
            boolean reserveJustMet = !auction.isReserveMet()
                    && auction.getReservePrice() != null
                    && auction.getCurrentBid().compareTo(auction.getReservePrice()) >= 0;

            AuctionTimingProperties.SoftClose sc = timing.getSoftClose();
            AuctionTimingProperties.FastFinish ff = timing.getFastFinish();

            long millisLeft = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();

            // 4-a  Soft-close anti-sniping
            if (sc.isEnabled() && millisLeft <= sc.getThresholdSeconds() * 1_000L) {
                auction.setEndTime(
                        auction.getEndTime().plusSeconds(sc.getExtensionSeconds()));
                lifecycleManager.scheduleAuctionEnd(auction);
                log.info("Anti-sniping: extended auction {} by {} s", auctionId, sc.getExtensionSeconds());
            }

            // 4-b  Reserve met ➜ optional fast-finish
            if (reserveJustMet) {
                auction.setReserveMet(true);

                if (auction.isFastFinishOnReserve() && ff.isEnabled()) {
                    long fastMs = ff.getFastFinishMinutes() * 60_000L;
                    if (millisLeft > fastMs) {   // shorten only if it would be earlier
                        auction.setEndTime(LocalDateTime.now().plusMinutes(ff.getFastFinishMinutes()));
                        lifecycleManager.scheduleAuctionEnd(auction);
                        log.info("Fast-finish: reserve met, new end for auction {} is {}", auctionId, auction.getEndTime());
                    }
                }
            }

            /* ---------- 5. Persist auction & publish event ----------------------------- */
            LiveAuction updatedAuction = liveAuctionRepository.save(auction);
            webSocketEventPublisher.publishAuctionStateUpdate(updatedAuction, newBid);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bid processing interrupted.");
        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    // --- Need blocking fetchBidderUsername helper ---
    private String fetchBidderUsername(String bidderId) {
        try {
            log.debug("Fetching username for bidder ID: {}", bidderId);
            Map<String, UserBasicInfoDto> users = userServiceClient.getUsersBasicInfoByIds(Collections.singletonList(bidderId));
            UserBasicInfoDto bidderInfo = users.get(bidderId);
            if (bidderInfo == null) { throw new UserNotFoundException("Bidder not found: " + bidderId); }
            return bidderInfo.getUsername();
        } catch (Exception e) {
            log.error("Failed to fetch username for bidder {}", bidderId, e);
            throw new UserNotFoundException("Bidder not found: " + bidderId);
        }
    }


    // --- getActiveAuctions (No change needed, uses Pageable from controller) ---
    @Override
    @Transactional(readOnly = true)
    public Page<LiveAuctionSummaryDto> getActiveAuctions(Pageable pageable) {
        log.debug("Fetching active auctions page: {}", pageable);
        Page<LiveAuction> auctionPage = liveAuctionRepository.findByStatus(AuctionStatus.ACTIVE, pageable);
        return auctionPage.map(auctionMapper::mapToLiveAuctionSummaryDto);
    }

    // LiveAuctionServiceImpl.java   (add below getActiveAuctions)
    @Override
    @Transactional(readOnly = true)
    public Page<LiveAuctionSummaryDto> getSellerAuctions(String sellerId,
                                                         AuctionStatus status,
                                                         Set<Long> categoryIds,
                                                         LocalDateTime from,
                                                         Pageable pageable) {

        Page<LiveAuction> page = liveAuctionRepository.findSellerAuctionsBySnapshot(
                sellerId,
                status,
                from,
                categoryIds == null ? Set.of() : categoryIds,
                categoryIds == null || categoryIds.isEmpty(),
                pageable);

        return page.map(auctionMapper::mapToLiveAuctionSummaryDto);
    }

    @Override
    @Transactional
    public void hammerDownNow(UUID auctionId, String sellerId) {

        LiveAuction auction = liveAuctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found "+auctionId));

        if (!auction.getSellerId().equals(sellerId))
            throw new InvalidAuctionStateException("Only the seller may hammer down");

        if (auction.getStatus() != AuctionStatus.ACTIVE)
            throw new InvalidAuctionStateException("Auction is not active");

        if (auction.getHighestBidderId() == null)
            throw new InvalidAuctionStateException("Cannot hammer – no bids yet");

        // publish to MQ — actual close happens in the manager
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.AUCTION_COMMAND_EXCHANGE,
                RabbitMqConfig.HAMMER_ROUTING_KEY,
                new AuctionLifecycleCommands.HammerDownCommand(auctionId, sellerId));
    }

    @Override
    @Transactional
    public void cancelAuction(UUID auctionId, String sellerId) {

        LiveAuction a = liveAuctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));

        if (!a.getSellerId().equals(sellerId))
            throw new InvalidAuctionStateException("Only the seller may cancel");

        if (!(a.getStatus() == AuctionStatus.SCHEDULED
                     || a.getStatus() == AuctionStatus.ACTIVE)) {
            throw new InvalidAuctionStateException("Only scheduled or active auctions can be cancelled");
        }

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.AUCTION_COMMAND_EXCHANGE,
                RabbitMqConfig.CANCEL_ROUTING_KEY,
                new AuctionLifecycleCommands.CancelAuctionCommand(auctionId, sellerId));
    }



}