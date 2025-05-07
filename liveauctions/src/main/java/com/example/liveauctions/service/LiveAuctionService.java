package com.example.liveauctions.service;

import com.example.liveauctions.dto.*;
import com.example.liveauctions.entity.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LiveAuctionService {

    LiveAuctionDetailsDto createAuction(String sellerId, CreateLiveAuctionDto createDto);

    LiveAuctionDetailsDto getAuctionDetails(UUID auctionId);

    void placeBid(UUID auctionId, String bidderId, PlaceBidDto bidDto);

    Page<LiveAuctionSummaryDto> getActiveAuctions(Pageable pageable);

    Page<LiveAuctionSummaryDto> getSellerAuctions(String sellerId,
                                                  AuctionStatus status,
                                                  Set<Long> categoryIds,
                                                  LocalDateTime from,
                                                  Pageable pageable);

    void hammerDownNow(UUID auctionId, String sellerId);

    void cancelAuction(UUID auctionId, String sellerId);

    /** Fetches summary details for a list of auction IDs. */
    List<LiveAuctionSummaryDto> getAuctionSummariesByIds(Set<UUID> auctionIds);

}