package com.example.liveauctions.service.impl;

import com.example.liveauctions.client.ProductServiceClient; // Feign Client for Products
import com.example.liveauctions.client.UserServiceClient; // Feign Client for Users
import com.example.liveauctions.client.dto.CategoryDto;
import com.example.liveauctions.client.dto.ProductDto; // DTO from Products service
import com.example.liveauctions.client.dto.UserBanStatusDto;
import com.example.liveauctions.client.dto.UserBasicInfoDto; // DTO from Users service
import com.example.liveauctions.commands.AuctionLifecycleCommands;
import com.example.liveauctions.config.AuctionTimingProperties;
import com.example.liveauctions.config.RabbitMqConfig; // Constants for RabbitMQ
import com.example.liveauctions.dto.*;
import com.example.liveauctions.dto.event.NewLiveAuctionFromReopenedOrderEventDto;
import com.example.liveauctions.entity.AuctionStatus;
import com.example.liveauctions.entity.Bid;
import com.example.liveauctions.entity.LiveAuction;
import com.example.liveauctions.exception.*;
import com.example.liveauctions.mapper.AuctionMapper;
import com.example.liveauctions.repository.BidRepository;
import com.example.liveauctions.repository.LiveAuctionRepository;
import com.example.liveauctions.service.LiveAuctionService;
import com.example.liveauctions.service.WebSocketEventPublisher; // For WebSocket events
import com.example.liveauctions.utils.DateTimeUtil;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.SetJoin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
    private final AuctionTimingProperties timing;
    private final LiveAuctionSchedulingServiceImpl schedulingService;

    // Assume getIncrement is available, maybe in a helper class or static method
    // private final AuctionHelper auctionHelper;

    @Override
    @Transactional
    public LiveAuctionDetailsDto createAuction(String sellerId, CreateLiveAuctionDto createDto) {
        log.info("Attempting to create live auction for product ID: {} by seller: {}", createDto.getProductId(), sellerId);

        // 1. Fetch Product & Seller Details (No change)
        ProductDto product = fetchProductDetails(createDto.getProductId());
        String sellerUsername = fetchSellerUsername(sellerId);

        // 2. Determine Timing & Status (No change)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = createDto.getStartTime() != null ? createDto.getStartTime() : now;
        // Calculate endTime
        LocalDateTime calculatedEndTime = startTime.plusMinutes(createDto.getDurationMinutes());
        // *** APPLY ROUNDING HERE ***
        LocalDateTime roundedEndTime = DateTimeUtil.roundToMicrosecond(calculatedEndTime);

        AuctionStatus initialStatus = startTime.isAfter(now) ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE;

        if (initialStatus == AuctionStatus.SCHEDULED && startTime.isBefore(now.plusSeconds(1))) { // Added a small buffer for "now"
            log.warn("Provided start time {} for product {} is in the past or too close to now. Starting auction now.", startTime, product.getId());
            startTime = DateTimeUtil.roundToMicrosecond(now); // Also round 'now' if used as startTime
            // Recalculate and round endTime if startTime changed to now
            roundedEndTime = DateTimeUtil.roundToMicrosecond(startTime.plusMinutes(createDto.getDurationMinutes()));
            initialStatus = AuctionStatus.ACTIVE;
        } else if (initialStatus == AuctionStatus.SCHEDULED) {
            startTime = DateTimeUtil.roundToMicrosecond(startTime); // Round scheduled start time
            // roundedEndTime is already calculated and rounded based on original or adjusted startTime
        }

        // 3. Calculate Initial Increment (No change)
        BigDecimal initialIncrement = getIncrement(createDto.getStartPrice());

        // 4. Snapshot Category IDs (No change)
        Set<Long> categoryIdsSnapshot = product.getCategories() == null ? Set.of() : product.getCategories().stream().map(CategoryDto::getId).collect(Collectors.toSet());

        // 5. Build LiveAuction Entity (No change)
        LiveAuction auction = LiveAuction.builder()
                .productId(product.getId()).sellerId(sellerId).productTitleSnapshot(product.getTitle())
                .productImageUrlSnapshot(product.getImageUrls() != null && !product.getImageUrls().isEmpty() ? product.getImageUrls().get(0) : null)
                .sellerUsernameSnapshot(sellerUsername).startPrice(createDto.getStartPrice()).reservePrice(createDto.getReservePrice())
                .currentBid(null).highestBidderId(null).highestBidderUsernameSnapshot(null).currentBidIncrement(initialIncrement)
                .startTime(startTime).endTime(roundedEndTime).status(initialStatus).reserveMet(false)
                .productCategoryIdsSnapshot(categoryIdsSnapshot)
                .build();

        // 6. Save the Auction Entity (No change)
        LiveAuction savedAuction = liveAuctionRepository.save(auction);
        log.info("Auction entity saved with ID: {} and status: {}", savedAuction.getId(), savedAuction.getStatus());

        // --- MODIFIED: 7. Schedule Start or Handle Immediate Start ---
        if (savedAuction.getStatus() == AuctionStatus.SCHEDULED) {
            // Delegate start scheduling to the service
            schedulingService.scheduleAuctionStart(savedAuction);
            log.info("Delegated scheduling start for auction {}", savedAuction.getId());
            // Note: The check for delayMillis > 0 is now inside the scheduling service
        } else if (savedAuction.getStatus() == AuctionStatus.ACTIVE) {
            // Auction starts immediately
            log.info("Auction {} starting immediately.", savedAuction.getId());
            // Delegate end scheduling to the service
            schedulingService.scheduleAuctionEnd(savedAuction);
            // Publish initial state update
            webSocketEventPublisher.publishAuctionStateUpdate(savedAuction, null); // Use publisher directly
        }
        // --- End MODIFICATION ---
        if (createDto.getOriginalOrderId() != null) {
            NewLiveAuctionFromReopenedOrderEventDto reopenEvent = NewLiveAuctionFromReopenedOrderEventDto.builder()
                    .eventId(UUID.randomUUID())
                    // eventTimestamp is defaulted in DTO
                    .newLiveAuctionId(savedAuction.getId())
                    .productId(savedAuction.getProductId())
                    .sellerId(savedAuction.getSellerId())
                    .originalOrderId(createDto.getOriginalOrderId())
                    .build();
            try {
                // Use the existing AUCTION_EVENTS_EXCHANGE (TopicExchange)
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.AUCTION_EVENTS_EXCHANGE,
                        RabbitMqConfig.AUCTION_LIVE_REOPENED_ORDER_CREATED_ROUTING_KEY,   // New, distinct routing key for live auction reopen
                        reopenEvent
                );
                log.info("Published NewLiveAuctionFromReopenedOrderEvent for original order ID: {}, new live auction ID: {}",
                        createDto.getOriginalOrderId(), savedAuction.getId());
            } catch (Exception e) {
                log.error("Failed to publish NewLiveAuctionFromReopenedOrderEvent for original order ID {}: {}",
                        createDto.getOriginalOrderId(), e.getMessage(), e);
                // Consider error handling strategy for event publishing failure
            }
        }

        // 8. Map to Details DTO and Return (No change)
        return auctionMapper.mapToLiveAuctionDetailsDto( savedAuction, product, Collections.emptyList(),
                Duration.between(LocalDateTime.now(), savedAuction.getEndTime()).toMillis(),
                // Calculate initial next bid correctly
                (savedAuction.getCurrentBid() == null ? savedAuction.getStartPrice() : savedAuction.getCurrentBid().add(savedAuction.getCurrentBidIncrement()))
        );
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

    private BigDecimal getIncrement(BigDecimal currentBid) {
        // If no bids yet, treat as 0 so we pick the first tier
        if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
            currentBid = BigDecimal.ZERO;
        }

        // Tiers (descending)
        if (currentBid.compareTo(new BigDecimal("50000000")) >= 0) {  // > 50 000 000
            return new BigDecimal("2000000");
        }
        if (currentBid.compareTo(new BigDecimal("20000000")) >= 0) {  // 20 000 000–50 000 000
            return new BigDecimal("1000000");
        }
        if (currentBid.compareTo(new BigDecimal("10000000")) >= 0) {  // 10 000 000–20 000 000
            return new BigDecimal("500000");
        }
        if (currentBid.compareTo(new BigDecimal("5000000")) >= 0) {   // 5 000 000–10 000 000
            return new BigDecimal("200000");
        }
        if (currentBid.compareTo(new BigDecimal("3000000")) >= 0) {   // 3 000 000–5 000 000
            return new BigDecimal("100000");
        }
        if (currentBid.compareTo(new BigDecimal("1000000")) >= 0) {   // 1 000 000–3 000 000
            return new BigDecimal("50000");
        }
        if (currentBid.compareTo(new BigDecimal("300000")) >= 0) {    // 300 000–1 000 000
            return new BigDecimal("10000");
        }
        if (currentBid.compareTo(new BigDecimal("100000")) >= 0) {    // 100 000–300 000
            return new BigDecimal("5000");
        }
        if (currentBid.compareTo(new BigDecimal("50000")) >= 0) {     // 50 000–100 000
            return new BigDecimal("1000");
        }
        // below 50 000
        return new BigDecimal("500");
    }


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
            if (!lockAcquired) throw new IllegalStateException("Could not process bid, please retry.");

            // --- BAN CHECK ---
            // Placed early after acquiring lock and before extensive processing.
            try {
                log.debug("Checking ban status for bidder {} for auction {}", bidderId, auctionId);
                UserBanStatusDto banStatus = userServiceClient.getUserBanStatus(bidderId);
                if (banStatus.isBanned()) {
                    log.warn("User {} is banned from bidding until {}. Bid rejected for auction {}.",
                            bidderId, banStatus.getBanEndsAt(), auctionId);
                    throw new UserBannedException("You are currently banned from bidding. Ban ends at: " + banStatus.getBanEndsAt());
                }
                log.debug("User {} is not banned. Proceeding with bid.", bidderId);
            } catch (UserBannedException e) {
                throw e; // Re-throw to be caught by controller advice
            } catch (Exception e) {
                // Handle Feign client errors (e.g., UsersService down)
                log.error("Failed to check ban status for user {}: {}. Applying fail-strict policy: Bid rejected.",
                        bidderId, e.getMessage());
                // Fail-strict: If ban status cannot be verified, reject the bid.
                throw new IllegalStateException("Could not verify bidding eligibility at this time. Please try again later.");
            }
            // --- END BAN CHECK ---

            LiveAuction auction = liveAuctionRepository.findById(auctionId)
                    .orElseThrow(() -> new AuctionNotFoundException("Auction not found: " + auctionId));
            LocalDateTime originalEndTime = auction.getEndTime(); // Store original end time before potential changes

            // 1. Validations (No change)
            validateAuctionStateForBidding(auction); // Uses local helper
            validateNotSeller(auction, bidderId);    // Uses local helper
            // ... validate bid amount ...
            BigDecimal currentBid = auction.getCurrentBid() == null ? BigDecimal.ZERO : auction.getCurrentBid();
            BigDecimal requiredAmount = (auction.getHighestBidderId() == null) ? auction.getStartPrice() : currentBid.add(auction.getCurrentBidIncrement());
            if (bidDto.getAmount().compareTo(requiredAmount) < 0) throw new InvalidBidException("Bid too low. Minimum required: " + requiredAmount);


            // 2. Persist new bid (No change)
            String bidderUsername = fetchBidderUsername(bidderId);
            Bid newBid = bidRepository.save(Bid.builder().liveAuctionId(auctionId).bidderId(bidderId)
                    .bidderUsernameSnapshot(bidderUsername).amount(bidDto.getAmount()).build());

            auction.setBidCount(auction.getBidCount() + 1);

            // 3. Update auction financials (No change)
            auction.setCurrentBid(bidDto.getAmount());
            auction.setHighestBidderId(bidderId);
            auction.setHighestBidderUsernameSnapshot(bidderUsername);
            auction.setCurrentBidIncrement(getIncrement(auction.getCurrentBid()));

            // 4. Reserve-met & timing rules
            boolean reserveJustMet = !auction.isReserveMet() && auction.getReservePrice() != null && auction.getCurrentBid().compareTo(auction.getReservePrice()) >= 0;
            AuctionTimingProperties.SoftClose sc = timing.getSoftClose();
            AuctionTimingProperties.FastFinish ff = timing.getFastFinish();
            LocalDateTime roundedNow = DateTimeUtil.roundToMicrosecond(LocalDateTime.now());
            long millisLeft = Duration.between(roundedNow, auction.getEndTime()).toMillis();
            boolean endTimeChanged = false;


            // 4-a Soft-close anti-sniping
            if (sc.isEnabled() && millisLeft > 0 && millisLeft <= sc.getThresholdSeconds() * 1_000L) {
                // CORRECT WAY: Extend from the auction's CURRENT end time
                LocalDateTime potentialNewEndTime = auction.getEndTime().plusSeconds(sc.getExtensionSeconds());
                // Update the auction's end time directly
                auction.setEndTime(potentialNewEndTime);
                endTimeChanged = true; // Mark for rescheduling
                log.info("Anti-sniping: extended auction {} by {}s to {}", auctionId, sc.getExtensionSeconds(), potentialNewEndTime);
            }

            // 4-b Reserve met -> optional fast-finish
            if (reserveJustMet) {
                auction.setReserveMet(true);
                log.info("Reserve price met for auction {}", auctionId);
                if (auction.isFastFinishOnReserve() && ff.isEnabled()) {
                    // Calculate potential earlier end time
                    LocalDateTime fastFinishEndTime = LocalDateTime.now().plusMinutes(ff.getFastFinishMinutes());
                    // Shorten only if fast finish time is EARLIER than current end time
                    if (fastFinishEndTime.isBefore(auction.getEndTime())) {
                        auction.setEndTime(fastFinishEndTime);
                        endTimeChanged = true; // Mark for rescheduling
                        log.info("Fast-finish: reserve met, new end for auction {} is {}", auctionId, auction.getEndTime());
                    }
                }
            }

            // --- MODIFIED: Reschedule end if necessary ---
            if (endTimeChanged) {
                // Use scheduling service to schedule/reschedule the end task
                schedulingService.scheduleAuctionEnd(auction);
            }
            // --- End MODIFICATION ---

            // 5. Persist auction & publish event (No change)
            LiveAuction updatedAuction = liveAuctionRepository.save(auction); // Save potentially updated auction
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

    private void validateAuctionStateForBidding(LiveAuction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new InvalidAuctionStateException("Auction is not active. Status: " + auction.getStatus());
        }
        // Use current time directly for end check
        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
            // This check might be slightly redundant if status is updated promptly by listener,
            // but it's good defense-in-depth within the placeBid operation itself.
            throw new InvalidAuctionStateException("Auction has already ended.");
        }
    }

    private void validateNotSeller(LiveAuction auction, String bidderId) {
        if (auction.getSellerId().equals(bidderId)) {
            throw new InvalidBidException("Seller cannot bid on their own auction.");
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
        LiveAuction auction = liveAuctionRepository.findById(auctionId) // Changed TimedAuction to LiveAuction
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found "+auctionId));
        if (!auction.getSellerId().equals(sellerId)) throw new InvalidAuctionStateException("Only the seller may hammer down");
        if (auction.getStatus() != AuctionStatus.ACTIVE) throw new InvalidAuctionStateException("Auction is not active");
        if (auction.getHighestBidderId() == null) throw new InvalidAuctionStateException("Cannot hammer – no bids yet");
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.AUCTION_COMMAND_EXCHANGE,
                RabbitMqConfig.HAMMER_ROUTING_KEY,
                new AuctionLifecycleCommands.HammerDownCommand(auctionId, sellerId));
        log.info("HammerDownCommand sent for auction {}", auctionId);
    }

    @Override
    @Transactional
    public void cancelAuction(UUID auctionId, String sellerId) {
        LiveAuction a = liveAuctionRepository.findById(auctionId) // Changed TimedAuction to LiveAuction
                .orElseThrow(() -> new AuctionNotFoundException("Auction not found"));
        if (!a.getSellerId().equals(sellerId)) throw new InvalidAuctionStateException("Only the seller may cancel");
        if (!(a.getStatus() == AuctionStatus.SCHEDULED || a.getStatus() == AuctionStatus.ACTIVE)) {
            throw new InvalidAuctionStateException("Only scheduled or active auctions can be cancelled");
        }
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.AUCTION_COMMAND_EXCHANGE,
                RabbitMqConfig.CANCEL_ROUTING_KEY,
                new AuctionLifecycleCommands.CancelAuctionCommand(auctionId, sellerId));
        log.info("CancelAuctionCommand sent for auction {}", auctionId);
    }

    // src/main/java/com/example/liveauctions/service/impl/LiveAuctionServiceImpl.java
// ... (imports and other code)

    @Override
    @Transactional(readOnly = true)
    public Page<LiveAuctionSummaryDto> searchAuctions(
            String queryText,
            Set<Long> categoryIds,
            AuctionStatus status, // This will be ACTIVE or SCHEDULED if 'ended' is not true
            Boolean ended,        // --- ADD THIS parameter ---
            LocalDateTime from,
            Pageable pageable) {

        log.debug("Service searching live auctions: query='{}', categories={}, status={}, ended={}, from={}, pageable={}",
                queryText, categoryIds, status, ended, from, pageable);

        Specification<LiveAuction> spec = (root, jpaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Text Search Predicate
            if (queryText != null && !queryText.isBlank()) {
                String lowercaseQuery = "%" + queryText.toLowerCase().trim() + "%";
                predicates.add(cb.like(cb.lower(root.get("productTitleSnapshot")), lowercaseQuery));
            }

            // Handle status and ended flag
            if (Boolean.TRUE.equals(ended)) {
                // If frontend filter is "Ended", query for terminal states for Live Auctions
                predicates.add(root.get("status").in(
                        AuctionStatus.SOLD,
                        AuctionStatus.CANCELLED,
                        AuctionStatus.RESERVE_NOT_MET
                ));
                // If 'ended' is true, any 'status' parameter for ACTIVE/SCHEDULED is ignored
            } else if (status != null) {
                // If a specific non-terminal status (ACTIVE, SCHEDULED) is provided
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startTime"), from));
            }

            if (categoryIds != null && !categoryIds.isEmpty()) {
                SetJoin<LiveAuction, Long> categoryJoin = root.joinSet("productCategoryIdsSnapshot", JoinType.INNER);
                predicates.add(categoryJoin.in(categoryIds));
                jpaQuery.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LiveAuction> auctionPage = liveAuctionRepository.findAll(spec, pageable);
        return auctionPage.map(auctionMapper::mapToLiveAuctionSummaryDto);
    }


    /**
     * Fetches summary details for a list of auction IDs.
     *
     * @param auctionIds
     */
    @Override
    @Transactional(readOnly = true)
    public List<LiveAuctionSummaryDto> getAuctionSummariesByIds(Set<UUID> auctionIds) {
        if (auctionIds == null || auctionIds.isEmpty()) {
            return Collections.emptyList();
        }
        log.debug("Fetching live auction summaries for IDs: {}", auctionIds);
        List<LiveAuction> auctions = liveAuctionRepository.findAllById(auctionIds); // Use JPA's built-in batch fetch
        return auctions.stream()
                .map(auctionMapper::mapToLiveAuctionSummaryDto) // Ensure mapper exists
                .collect(Collectors.toList());
    }

}