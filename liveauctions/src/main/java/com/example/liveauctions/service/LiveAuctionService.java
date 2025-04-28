package com.example.liveauctions.service;

import com.example.liveauctions.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LiveAuctionService {

    LiveAuctionDetailsDto createAuction(String sellerId, CreateLiveAuctionDto createDto);

    LiveAuctionDetailsDto getAuctionDetails(UUID auctionId);

    void placeBid(UUID auctionId, String bidderId, PlaceBidDto bidDto);

    Page<LiveAuctionSummaryDto> getActiveAuctions(Pageable pageable);

    // We'll need internal methods for starting/ending auctions, calculating increments, etc.
    // And potentially methods called by scheduled tasks or message listeners.
}