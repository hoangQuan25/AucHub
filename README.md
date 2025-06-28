graph TD
    subgraph "Auction End"
        A[Auction Ends & Winner is Declared]
    end

    subgraph "Payment Phase"
        B(Order Created: Awaiting Payment)
        C{Winner Pays within 48h?}
    end

    A --> B --> C

    %% Happy Path - Winner Pays
    subgraph "Successful Order Fulfillment"
        D[Payment Confirmed]
        E[Seller Ships Item & Provides Tracking]
        F[Item Delivered: 7-Day Buyer Window Starts]
        G{Buyer Takes Action within 7 Days?}
        H[Order Complete & Payment Released to Seller]
        I[Return Process Initiated]
        J[Order Auto-Completed After 7 Days]
    end

    C -- Yes --> D
    D --> E
    E --> F
    F --> G
    G -- "Yes, Confirms Receipt" --> H
    G -- "Yes, Requests Return" --> I
    G -- "No Action Taken" --> J


    %% Default Path - Winner Does Not Pay
    subgraph "Winner Default & Second Chance"
        K[Winner Defaults on Payment]
        L[Penalty Applied to Winner's Account (Strike/Ban)]
        M{Seller's Choice?}
        N[Seller offers to Next Bidder]
        O{Next Bidder Pays within 48h?}
        P[Seller Re-lists Item in a New Auction]
    end

    C -- No --> K
    K --> L
    L --> M
    M -- "Offer Second Chance" --> N
    M -- "Re-list Item" --> P
    N --> O
    O -- "Yes" --> D
    O -- "No" --> P


    %% Styling
    classDef startEnd fill:#d4edda,stroke:#155724,stroke-width:2px;
    classDef decision fill:#fff3cd,stroke:#856404,stroke-width:2px;
    classDef process fill:#cce5ff,stroke:#004085,stroke-width:2px;
    classDef penalty fill:#f8d7da,stroke:#721c24,stroke-width:2px;

    class A,H,I,J,P startEnd;
    class C,G,M,O decision;
    class B,D,E,F,K,N process;
    class L penalty;
