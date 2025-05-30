// src/pages/HomePage.jsx
import React, { useState, useEffect, useCallback } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  FaBook,
  FaChair,
  FaTshirt,
  FaBolt,
  FaGem,
  FaSearch,
  FaGavel,
  FaShoppingCart,
  FaChartLine,
  FaUsers,
  FaShieldAlt,
  FaClock,
} from "react-icons/fa"; // Adjusted icons for categories

// Import your actual components
import AuctionCard from "../components/AuctionCard"; // Assuming path
import CountdownTimer from "../components/CountdownTimer"; // Assuming path
import apiClient from "../api/apiClient"; // Assuming path
import InteractiveAuctionCard from "../components/InteractiveAuctionCard";

const mainCategoryData = [
  {
    id: 1,
    name: "Books",
    Icon: FaBook,
    description: "Comics, Novels, Educational & More",
    link: "/search?categories=1",
  },
  {
    id: 2,
    name: "Furniture",
    Icon: FaChair,
    description: "Seating, Tables, Storage Solutions",
    link: "/search?categories=2",
  },
  {
    id: 3,
    name: "Fashion & Costume",
    Icon: FaTshirt,
    description: "Apparel, Shoes, Accessories, Cosplay",
    link: "/search?categories=3",
  },
  {
    id: 4,
    name: "Electronics (Limited)",
    Icon: FaBolt,
    description: "Small Appliances, Audio",
    link: "/search?categories=4",
  },
  {
    id: 5,
    name: "Collectibles & Hobbies",
    Icon: FaGem,
    description: "Cards, Stamps, Toys, Memorabilia",
    link: "/search?categories=5",
  },
];

// Helper functions for banner text (as discussed)
const getBannerAuctionStatusText = (auction) => {
  if (!auction || !auction.status) return "Trạng thái không xác định";
  switch (auction.status.toUpperCase()) {
    case "ACTIVE":
      return auction.auctionType === "LIVE" ? "TRỰC TIẾP" : "ĐANG DIỄN RA";
    case "SCHEDULED":
      return "SẮP DIỄN RA";
    case "SOLD":
      return "ĐÃ BÁN";
    case "ENDED":
    case "RESERVE_NOT_MET":
      return "ĐÃ KẾT THÚC";
    case "CANCELLED":
      return "ĐÃ HỦY";
    default:
      return auction.status;
  }
};

const getBannerAuctionPriceInfo = (auction) => {
  if (!auction) return "";
  let priceLabel = "Giá khởi điểm:";
  let price = auction.startPrice ?? 0;
  if (auction.status === "ACTIVE" || auction.status === "SOLD") {
    priceLabel = "Giá hiện tại:";
    price = auction.currentBid ?? auction.startPrice ?? 0;
  } else if (auction.status === "SCHEDULED") {
    priceLabel = "Giá khởi điểm:";
    price = auction.startPrice ?? 0;
  } else {
    priceLabel = "Giá:";
    price = auction.currentBid ?? auction.startPrice ?? 0;
  }
  return `${priceLabel} ${price.toLocaleString("vi-VN")} VNĐ`;
};

function HomePage() {
  const navigate = useNavigate();

  // --- State for Dynamic Sections ---
  const [bannerAuctions, setBannerAuctions] = useState([]);
  const [currentBannerIndex, setCurrentBannerIndex] = useState(0);
  const [loadingBanner, setLoadingBanner] = useState(true);
  const [errorBanner, setErrorBanner] = useState("");

  // Define mainCategoryData directly or fetch if it becomes dynamic
  const [featuredCategories, setFeaturedCategories] = useState([]); // Will be set from mainCategoryData
  const [loadingCategories, setLoadingCategories] = useState(true); // Still good to have for async pattern
  const [errorCategories, setErrorCategories] = useState("");

  const [endingSoonAuctions, setEndingSoonAuctions] = useState([]);
  const [loadingEndingSoon, setLoadingEndingSoon] = useState(true);
  const [errorEndingSoon, setErrorEndingSoon] = useState("");

  const [startingSoonAuctions, setStartingSoonAuctions] = useState([]);
  const [loadingStartingSoon, setLoadingStartingSoon] = useState(true);
  const [errorStartingSoon, setErrorStartingSoon] = useState("");

  const [hotAuctions, setHotAuctions] = useState([]);
  const [loadingHot, setLoadingHot] = useState(true);
  const [errorHot, setErrorHot] = useState("");

  const [checkOutLiveAuctions, setCheckOutLiveAuctions] = useState([]);
  const [loadingCheckOutLive, setLoadingCheckOutLive] = useState(true);
  const [errorCheckOutLive, setErrorCheckOutLive] = useState("");

  const [checkOutTimedAuctions, setCheckOutTimedAuctions] = useState([]);
  const [loadingCheckOutTimed, setLoadingCheckOutTimed] = useState(true);
  const [errorCheckOutTimed, setErrorCheckOutTimed] = useState("");

  // --- Fetching Logic ---
  const fetchFeaturedCategories = useCallback(async () => {
    setLoadingCategories(true);
    setErrorCategories("");
    try {
      // Simulate API call for consistency, directly use mainCategoryData
      await new Promise((resolve) => setTimeout(resolve, 100)); // Simulate async
      setFeaturedCategories(mainCategoryData);
    } catch (err) {
      console.error("Error setting featured categories:", err);
      setErrorCategories("Could not load categories.");
    } finally {
      setLoadingCategories(false);
    }
  }, []); // Add mainCategoryData if it could change, though it's const here

  const fetchBannerAuctions = useCallback(async (count = 3) => {
    setLoadingBanner(true);
    setErrorBanner("");
    try {
      const liveParams = {
        page: 0,
        size: 2,
        status: "ACTIVE,SCHEDULED",
        sort: "bidCount,desc",
      };
      const timedParams = {
        page: 0,
        size: 2,
        status: "ACTIVE,SCHEDULED",
        sort: "bidCount,desc",
      };
      const liveFallbackParams = {
        page: 0,
        size: 2,
        status: "ACTIVE,SCHEDULED",
        sort: "startTime,desc",
      };
      const timedFallbackParams = {
        page: 0,
        size: 2,
        status: "ACTIVE,SCHEDULED",
        sort: "startTime,desc",
      };

      const [liveRes, timedRes] = await Promise.allSettled([
        apiClient
          .get("/liveauctions/search", { params: liveParams })
          .catch(() =>
            apiClient.get("/liveauctions/search", {
              params: liveFallbackParams,
            })
          ),
        apiClient
          .get("/timedauctions/search", { params: timedParams })
          .catch(() =>
            apiClient.get("/timedauctions/search", {
              params: timedFallbackParams,
            })
          ),
      ]);

      let bannerItems = [];
      if (
        liveRes.status === "fulfilled" &&
        liveRes.value.data?.content?.length > 0
      ) {
        bannerItems.push(
          ...liveRes.value.data.content.map((a) => ({
            ...a,
            auctionType: "LIVE",
          }))
        );
      }
      if (
        timedRes.status === "fulfilled" &&
        timedRes.value.data?.content?.length > 0
      ) {
        bannerItems.push(
          ...timedRes.value.data.content.map((a) => ({
            ...a,
            auctionType: "TIMED",
          }))
        );
      }

      if (bannerItems.length === 0) {
        // Broader fallback if primary attempts yield nothing
        const anyLiveParams = {
          page: 0,
          size: 2,
          status: "ACTIVE",
          sort: "endTime,asc",
        };
        const anyTimedParams = {
          page: 0,
          size: 2,
          status: "ACTIVE",
          sort: "endTime,asc",
        };
        const [anyLiveRes, anyTimedRes] = await Promise.allSettled([
          apiClient.get("/liveauctions/search", { params: anyLiveParams }),
          apiClient.get("/timedauctions/search", { params: anyTimedParams }),
        ]);
        if (
          anyLiveRes.status === "fulfilled" &&
          anyLiveRes.value.data?.content?.length > 0
        ) {
          bannerItems.push(
            ...anyLiveRes.value.data.content.map((a) => ({
              ...a,
              auctionType: "LIVE",
            }))
          );
        }
        if (
          anyTimedRes.status === "fulfilled" &&
          anyTimedRes.value.data?.content?.length > 0
        ) {
          bannerItems.push(
            ...anyTimedRes.value.data.content.map((a) => ({
              ...a,
              auctionType: "TIMED",
            }))
          );
        }
      }
      setBannerAuctions(
        bannerItems.sort(() => 0.5 - Math.random()).slice(0, count)
      );
    } catch (err) {
      console.error("Failed to fetch banner auctions:", err);
      setErrorBanner("Could not load banner auctions.");
    } finally {
      setLoadingBanner(false);
    }
  }, []);

  const fetchStartingSoonAuctions = useCallback(
    async (countPerType = 2, totalCount = 4) => {
      setLoadingStartingSoon(true);
      setErrorStartingSoon("");
      try {
        const commonParams = {
          page: 0,
          size: countPerType,
          status: "SCHEDULED", // Key change: fetch SCHEDULED auctions
          sort: "startTime,asc", // Sort by start time, ascending (earliest first)
        };

        const [liveRes, timedRes] = await Promise.allSettled([
          apiClient.get("/liveauctions/search", { params: commonParams }),
          apiClient.get("/timedauctions/search", { params: commonParams }),
        ]);

        const liveAuctionsData =
          liveRes.status === "fulfilled"
            ? (liveRes.value.data?.content || []).map((a) => ({
                ...a,
                auctionType: "LIVE",
              }))
            : [];
        const timedAuctionsData =
          timedRes.status === "fulfilled"
            ? (timedRes.value.data?.content || []).map((a) => ({
                ...a,
                auctionType: "TIMED",
              }))
            : [];

        const combined = [...liveAuctionsData, ...timedAuctionsData].sort(
          (a, b) =>
            new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
        );
        setStartingSoonAuctions(combined.slice(0, totalCount));
      } catch (err) {
        console.error("Failed to fetch starting soon auctions:", err);
        setErrorStartingSoon("Could not load auctions starting soon.");
      } finally {
        setLoadingStartingSoon(false);
      }
    },
    []
  );

  const fetchEndingSoonAuctions = useCallback(
    async (countPerType = 2, totalCount = 4) => {
      setLoadingEndingSoon(true);
      setErrorEndingSoon("");
      try {
        const liveParams = {
          page: 0,
          size: countPerType,
          sort: "endTime,asc",
          status: "ACTIVE",
        };
        const timedParams = {
          page: 0,
          size: countPerType,
          sort: "endTime,asc",
          status: "ACTIVE",
        };
        const [liveRes, timedRes] = await Promise.allSettled([
          apiClient.get("/liveauctions/search", { params: liveParams }),
          apiClient.get("/timedauctions/search", { params: timedParams }),
        ]);
        const liveAuctionsData =
          liveRes.status === "fulfilled"
            ? (liveRes.value.data?.content || []).map((a) => ({
                ...a,
                auctionType: "LIVE",
              }))
            : [];
        const timedAuctionsData =
          timedRes.status === "fulfilled"
            ? (timedRes.value.data?.content || []).map((a) => ({
                ...a,
                auctionType: "TIMED",
              }))
            : [];
        const combined = [...liveAuctionsData, ...timedAuctionsData].sort(
          (a, b) =>
            new Date(a.endTime).getTime() - new Date(b.endTime).getTime()
        );
        setEndingSoonAuctions(combined.slice(0, totalCount));
      } catch (err) {
        console.error("Failed to fetch ending soon auctions:", err);
        setErrorEndingSoon("Could not load auctions ending soon.");
      } finally {
        setLoadingEndingSoon(false);
      }
    },
    []
  );

  const fetchHotAuctions = useCallback(
    async (countPerType = 2, totalCount = 4) => {
      setLoadingHot(true);
      setErrorHot("");
      try {
        const commonParams = {
          page: 0,
          size: countPerType,
          status: "ACTIVE",
          sort: "bidCount,desc",
        };
        const fallbackParams = {
          page: 0,
          size: countPerType,
          status: "ACTIVE",
          sort: "startTime,desc",
        }; // Fallback sort

        const [liveRes, timedRes] = await Promise.allSettled([
          apiClient
            .get("/liveauctions/search", { params: commonParams })
            .catch(() =>
              apiClient.get("/liveauctions/search", { params: fallbackParams })
            ),
          apiClient
            .get("/timedauctions/search", { params: commonParams })
            .catch(() =>
              apiClient.get("/timedauctions/search", { params: fallbackParams })
            ),
        ]);
        const liveAuctionsData =
          liveRes.status === "fulfilled"
            ? (liveRes.value.data?.content || []).map((a) => ({
                ...a,
                auctionType: "LIVE",
              }))
            : [];
        const timedAuctionsData =
          timedRes.status === "fulfilled"
            ? (timedRes.value.data?.content || []).map((a) => ({
                ...a,
                auctionType: "TIMED",
              }))
            : [];
        const combined = [...liveAuctionsData, ...timedAuctionsData].sort(
          (a, b) => (b.bidCount || 0) - (a.bidCount || 0)
        );
        setHotAuctions(combined.slice(0, totalCount));
      } catch (err) {
        console.error("Failed to fetch hot auctions:", err);
        setErrorHot("Could not load hot auctions.");
      } finally {
        setLoadingHot(false);
      }
    },
    []
  );

  const fetchCheckOutLiveAuctions = useCallback(async (count = 6) => {
    setLoadingCheckOutLive(true);
    setErrorCheckOutLive("");
    try {
      const params = {
        page: 0,
        size: count,
        status: "ACTIVE",
        sort: "startTime,desc",
      };
      const response = await apiClient.get("/liveauctions/search", { params });
      setCheckOutLiveAuctions(
        (response.data?.content || []).map((a) => ({
          ...a,
          auctionType: "LIVE",
        }))
      );
    } catch (err) {
      console.error("Failed to fetch 'check out' live auctions:", err);
      setErrorCheckOutLive("Could not load these live auctions.");
    } finally {
      setLoadingCheckOutLive(false);
    }
  }, []);

  const fetchCheckOutTimedAuctions = useCallback(async (count = 6) => {
    setLoadingCheckOutTimed(true);
    setErrorCheckOutTimed("");
    try {
      const params = {
        page: 0,
        size: count,
        status: "ACTIVE",
        sort: "startTime,desc",
      };
      const response = await apiClient.get("/timedauctions/search", { params });
      setCheckOutTimedAuctions(
        (response.data?.content || []).map((a) => ({
          ...a,
          auctionType: "TIMED",
        }))
      );
    } catch (err) {
      console.error("Failed to fetch 'check out' timed auctions:", err);
      setErrorCheckOutTimed("Could not load these timed auctions.");
    } finally {
      setLoadingCheckOutTimed(false);
    }
  }, []);

  // Main useEffect to call all fetch functions
  useEffect(() => {
    const fetchAllHomePageData = async () => {
      // Using Promise.allSettled to ensure all fetches attempt to run
      // even if some fail.
      await Promise.allSettled([
        fetchBannerAuctions(),
        fetchFeaturedCategories(),
        fetchStartingSoonAuctions(),
        fetchEndingSoonAuctions(),
        fetchHotAuctions(),
        fetchCheckOutLiveAuctions(),
        fetchCheckOutTimedAuctions(),
      ]);
    };
    fetchAllHomePageData();
  }, [
    fetchBannerAuctions,
    fetchFeaturedCategories,
    fetchStartingSoonAuctions,
    fetchEndingSoonAuctions,
    fetchHotAuctions,
    fetchCheckOutLiveAuctions,
    fetchCheckOutTimedAuctions,
  ]);

  // Banner rotation effect
  useEffect(() => {
    if (bannerAuctions.length > 1) {
      const timer = setTimeout(() => {
        setCurrentBannerIndex((prevIndex) =>
          prevIndex === bannerAuctions.length - 1 ? 0 : prevIndex + 1
        );
      }, 5000); // Change banner every 5 seconds
      return () => clearTimeout(timer);
    }
  }, [currentBannerIndex, bannerAuctions.length]);

  const currentBannerItem =
    bannerAuctions.length > 0 ? bannerAuctions[currentBannerIndex] : null;

  // --- JSX Structure ---
  return (
    <div className="bg-gray-100 min-h-screen">
      {/* 1. Hero Section with Banner (Placeholder for now) */}
      <section className="hero-banner relative w-full h-[300px] sm:h-[400px] md:h-[500px] bg-gray-700 text-white overflow-hidden">
        {loadingBanner && (
          <div className="flex justify-center items-center h-full">
            <p>Loading banner...</p>
          </div>
        )}
        {errorBanner && (
          <div className="flex justify-center items-center h-full">
            <p className="text-red-400">{errorBanner}</p>
          </div>
        )}
        {!loadingBanner && !errorBanner && currentBannerItem && (
          <div
            key={currentBannerItem.id}
            className="absolute inset-0 w-full h-full transition-opacity duration-1000 ease-in-out opacity-100" // Simplified: always show current
            style={{
              backgroundImage: `url(${
                currentBannerItem.productImageUrlSnapshot || "/placeholder.png"
              })`,
              backgroundSize: "cover",
              backgroundPosition: "center",
            }}
          >
            <div className="absolute inset-0 bg-black bg-opacity-40 flex flex-col justify-center items-center text-center p-4">
              <h1
                className="text-2xl sm:text-3xl md:text-4xl font-bold mb-2 sm:mb-3 shadow-md"
                title={currentBannerItem.productTitleSnapshot}
              >
                {currentBannerItem.productTitleSnapshot}
              </h1>
              <p className="text-sm sm:text-md md:text-lg mb-1 sm:mb-2 text-yellow-300 font-semibold">
                {getBannerAuctionStatusText(currentBannerItem)}
              </p>
              <p className="text-sm sm:text-md mb-3 sm:mb-4">
                {getBannerAuctionPriceInfo(currentBannerItem)}
              </p>
              <Link
                to={`/${currentBannerItem.auctionType.toLowerCase()}-auctions/${
                  currentBannerItem.id
                }`}
                className="bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2 sm:py-3 px-4 sm:px-6 rounded-md text-sm sm:text-md transition duration-150"
              >
                Xem Chi Tiết {/* Or more dynamic CTA */}
              </Link>
            </div>
          </div>
        )}
        {!loadingBanner && !errorBanner && bannerAuctions.length > 1 && (
          <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 flex space-x-2">
            {bannerAuctions.map((_, index) => (
              <button
                key={`dot-${index}`}
                onClick={() => setCurrentBannerIndex(index)}
                className={`w-3 h-3 rounded-full transition-colors ${
                  index === currentBannerIndex
                    ? "bg-white"
                    : "bg-white/50 hover:bg-white/75"
                }`}
                aria-label={`Go to slide ${index + 1}`}
              />
            ))}
          </div>
        )}
        {!loadingBanner && !errorBanner && bannerAuctions.length === 0 && (
          <div className="flex justify-center items-center h-full">
            <p>No banner auctions to display.</p>
          </div>
        )}
      </section>

      {/* 2. Featured Categories Section */}
      <section className="py-8 sm:py-12 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl sm:text-3xl font-bold text-center text-gray-800 mb-6 sm:mb-8">
            Khám Phá Danh Mục
          </h2>
          {loadingCategories && (
            <p className="text-center">Loading categories...</p>
          )}
          {errorCategories && (
            <p className="text-center text-red-500">{errorCategories}</p>
          )}
          {!loadingCategories &&
            !errorCategories &&
            featuredCategories.length > 0 && (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 sm:gap-6">
                {featuredCategories.map((category) => (
                  <Link
                    key={category.id}
                    to={category.link}
                    className="group flex flex-col items-center p-4 sm:p-6 border border-gray-200 rounded-lg hover:shadow-xl hover:border-indigo-300 transition-all duration-300 ease-in-out transform hover:-translate-y-1"
                  >
                    <category.Icon className="text-3xl sm:text-4xl text-indigo-600 mb-2 sm:mb-3 group-hover:text-indigo-700 transition-colors" />
                    <h3 className="text-sm sm:text-md font-semibold text-gray-700 group-hover:text-indigo-700 transition-colors text-center">
                      {category.name}
                    </h3>
                    <p className="text-xs text-gray-500 group-hover:text-gray-600 mt-1 text-center hidden sm:block">
                      {category.description}
                    </p>
                  </Link>
                ))}
              </div>
            )}
          {!loadingCategories &&
            !errorCategories &&
            featuredCategories.length === 0 && (
              <p className="text-center text-gray-500">
                No categories to display.
              </p>
            )}
        </div>
      </section>

      {/* 3. "Check these out!" Section */}
      <section className="py-8 sm:py-12 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-8 text-center">
            Hãy Xem Qua Những Phiên Đấu Giá Này!
          </h2>

          {/* Timed Auctions Subsection */}
          <div className="mb-10">
            <h3 className="text-xl font-semibold text-gray-700 mb-4 flex items-center">
              <FaGavel className="text-blue-600 mr-2 transform -scale-x-100" />{" "}
              {/* Icon for Timed */}
              Phiên Đấu Giá Có Giới Hạn Thời Gian
            </h3>
            {loadingCheckOutTimed && <p>Loading timed auctions...</p>}
            {errorCheckOutTimed && (
              <p className="text-red-500">{errorCheckOutTimed}</p>
            )}
            {!loadingCheckOutTimed &&
              !errorCheckOutTimed &&
              checkOutTimedAuctions.length === 0 && (
                <p>Không có phiên đấu giá nào.</p>
              )}
            {!loadingCheckOutTimed &&
              !errorCheckOutTimed &&
              checkOutTimedAuctions.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
                  {" "}
                  {/* 3 per row for 6 items */}
                  {checkOutTimedAuctions.map((auction) => (
                    <InteractiveAuctionCard
                      key={auction.id}
                      auction={auction}
                      type={auction.auctionType}
                    />
                  ))}
                </div>
              )}
          </div>

          {/* Live Auctions Subsection */}
          <div>
            <h3 className="text-xl font-semibold text-gray-700 mb-4 flex items-center">
              <FaBolt className="text-red-500 mr-2" /> {/* Icon for Live */}
              Phiên Đấu Giá Trực Tiếp
            </h3>
            {loadingCheckOutLive && <p>Loading live auctions...</p>}
            {errorCheckOutLive && (
              <p className="text-red-500">{errorCheckOutLive}</p>
            )}
            {!loadingCheckOutLive &&
              !errorCheckOutLive &&
              checkOutLiveAuctions.length === 0 && (
                <p>Không có phiên đấu giá nào.</p>
              )}
            {!loadingCheckOutLive &&
              !errorCheckOutLive &&
              checkOutLiveAuctions.length > 0 && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
                  {" "}
                  {/* 3 per row for 6 items */}
                  {checkOutLiveAuctions.map((auction) => (
                    <InteractiveAuctionCard
                      key={auction.id}
                      auction={auction}
                      type={auction.auctionType}
                    />
                  ))}
                </div>
              )}
          </div>
        </div>
      </section>

      <section className="py-8 sm:py-12 bg-white"> {/* Or bg-gray-50 for alternation */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center mb-6 sm:mb-8">
            <h2 className="text-2xl sm:text-3xl font-bold text-gray-800 flex items-center">
              <FaClock className="text-green-500 mr-3" /> {/* Icon for Starting Soon */}
              Sắp Bắt Đầu
            </h2>
            <Link
              to="/search?status=SCHEDULED&sort=startTime,asc" // Link to all scheduled auctions
              className="text-sm font-medium text-indigo-600 hover:text-indigo-800 transition-colors"
            >
              Xem Tất Cả &rarr;
            </Link>
          </div>
          {loadingStartingSoon && (
            <p className="text-center">Loading auctions starting soon...</p>
          )}
          {errorStartingSoon && (
            <p className="text-center text-red-500">{errorStartingSoon}</p>
          )}
          {!loadingStartingSoon &&
            !errorStartingSoon &&
            startingSoonAuctions.length > 0 && (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {startingSoonAuctions.map((auction) => (
                  <InteractiveAuctionCard
                    key={auction.id}
                    auction={auction}
                    type={auction.auctionType} // Make sure auctionType is added in fetch
                  />
                ))}
              </div>
            )}
          {!loadingStartingSoon &&
            !errorStartingSoon &&
            startingSoonAuctions.length === 0 && (
              <p className="text-center text-gray-500">
                Không có phiên đấu giá nào sắp bắt đầu.
              </p>
            )}
        </div>
      </section>

      {/* 4. "Ending Soon" / "Hot Right Now" Auctions Section */}
      <section className="py-8 sm:py-12 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center mb-6 sm:mb-8">
            <h2 className="text-2xl sm:text-3xl font-bold text-gray-800 flex items-center">
              <FaClock className="text-red-500 mr-3" /> Sắp Kết Thúc / Đang Hot
            </h2>
            <Link
              to="/search?status=ACTIVE&sort=endTime,asc"
              className="text-sm font-medium text-indigo-600 hover:text-indigo-800 transition-colors"
            >
              Xem Tất Cả &rarr;
            </Link>
          </div>
          {(loadingEndingSoon || loadingHot) && (
            <p>Loading featured auctions...</p>
          )}
          {errorEndingSoon && <p className="text-red-500">{errorEndingSoon}</p>}
          {errorHot && <p className="text-red-500">{errorHot}</p>}

          {/* Combine endingSoon and hotAuctions, ensuring no duplicates and then display */}
          {/* This part needs a bit more logic to combine and de-duplicate if needed */}
          {/* For now, let's just show them sequentially or interleave if possible */}
          {!loadingEndingSoon &&
          !loadingHot &&
          (endingSoonAuctions.length > 0 || hotAuctions.length > 0) ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
              {/* A simple way to combine and avoid duplicates by ID, then take top N */}
              {Array.from(
                new Map(
                  [...endingSoonAuctions, ...hotAuctions].map((a) => [a.id, a])
                ).values()
              )
                .slice(0, 4) // Show up to 4 unique auctions from these combined lists
                .map((auction) => (
                  <InteractiveAuctionCard
                    key={auction.id}
                    auction={auction}
                    type={auction.auctionType}
                  />
                ))}
            </div>
          ) : (
            !loadingEndingSoon &&
            !loadingHot && <p>No featured auctions to display at the moment.</p>
          )}
        </div>
      </section>

      {/* 5. "How It Works" Section (Static) - From mock */}
      <section className="py-8 sm:py-12 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl sm:text-3xl font-bold text-center text-gray-800 mb-6 sm:mb-8">
            Cách Thức Hoạt Động
          </h2>
          <div className="grid md:grid-cols-3 gap-6 sm:gap-8 text-center">
            <div className="p-4">
              <FaSearch className="text-4xl text-indigo-600 mx-auto mb-3" />
              <h3 className="text-lg font-semibold text-gray-700 mb-1">
                1. Tìm Kiếm
              </h3>
              <p className="text-sm text-gray-600">
                Khám phá hàng ngàn sản phẩm độc đáo.
              </p>
            </div>
            <div className="p-4">
              <FaGavel className="text-4xl text-indigo-600 mx-auto mb-3" />
              <h3 className="text-lg font-semibold text-gray-700 mb-1">
                2. Đặt Giá
              </h3>
              <p className="text-sm text-gray-600">
                Tham gia đấu giá và đặt mức giá tốt nhất của bạn.
              </p>
            </div>
            <div className="p-4">
              <FaShoppingCart className="text-4xl text-indigo-600 mx-auto mb-3" />
              <h3 className="text-lg font-semibold text-gray-700 mb-1">
                3. Chiến Thắng & Sở Hữu
              </h3>
              <p className="text-sm text-gray-600">
                Thanh toán an toàn và nhận sản phẩm.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 6. "Why Choose AucHub?" Section (Static) - From mock */}
      <section className="py-8 sm:py-12 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl sm:text-3xl font-bold text-center text-gray-800 mb-6 sm:mb-8">
            Tại Sao Chọn Chúng Tôi?
          </h2>
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 text-center">
            <div className="p-4">
              <FaUsers className="text-3xl text-indigo-600 mx-auto mb-2" />
              <h4 className="text-md font-semibold">Cộng Đồng Sôi Nổi</h4>
              <p className="text-xs text-gray-500">
                Kết nối người mua và người bán đam mê.
              </p>
            </div>
            <div className="p-4">
              <FaShieldAlt className="text-3xl text-indigo-600 mx-auto mb-2" />
              <h4 className="text-md font-semibold">Giao Dịch An Toàn</h4>
              <p className="text-xs text-gray-500">
                Nền tảng bảo mật, thanh toán minh bạch.
              </p>
            </div>
            <div className="p-4">
              <FaChartLine className="text-3xl text-indigo-600 mx-auto mb-2" />
              <h4 className="text-md font-semibold">Giá Trị Tốt Nhất</h4>
              <p className="text-xs text-gray-500">
                Cơ hội sở hữu sản phẩm độc đáo với giá hời.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default HomePage;
