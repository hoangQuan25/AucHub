// src/pages/AuctionRulesGuidePage.jsx
import React from 'react';
import { FaBookOpen, FaGavel, FaClock, FaUsers, FaShieldAlt, FaBolt, FaDollarSign } from 'react-icons/fa'; // Example icons

const AuctionRulesGuidePage = () => {
  const commonRulesPillStyle = "bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium inline-block mr-2 mb-2";
  const liveAuctionPillStyle = "bg-red-100 text-red-800 px-3 py-1 rounded-full text-sm font-medium inline-block mr-2 mb-2";
  const timedAuctionPillStyle = "bg-green-100 text-green-800 px-3 py-1 rounded-full text-sm font-medium inline-block mr-2 mb-2";
  const sectionTitleStyle = "text-3xl font-bold text-gray-800 mb-6 pb-2 border-b-2 border-indigo-500 flex items-center";
  const subTitleStyle = "text-2xl font-semibold text-gray-700 mb-4 mt-6";
  const paragraphStyle = "text-gray-700 leading-relaxed mb-4";
  const listItemStyle = "ml-5 list-disc text-gray-700 mb-2";
  const placeholderImageStyle = "w-full h-64 bg-gray-300 rounded-lg flex items-center justify-center text-gray-500 text-xl font-semibold my-6 shadow";

  const bidIncrementData = [
    { range: "< 50,000 VNĐ", increment: "5,000 VNĐ" },
    { range: "50,000 – 99,999 VNĐ", increment: "10,000 VNĐ" },
    { range: "100,000 – 199,999 VNĐ", increment: "20,000 VNĐ" },
    { range: "200,000 – 499,999 VNĐ", increment: "50,000 VNĐ" },
    { range: "500,000 – 999,999 VNĐ", increment: "100,000 VNĐ" },
    { range: "1,000,000 – 1,999,999 VNĐ", increment: "200,000 VNĐ" },
    { range: "2,000,000 – 4,999,999 VNĐ", increment: "500,000 VNĐ" },
    { range: "5,000,000 – 9,999,999 VNĐ", increment: "1,000,000 VNĐ" },
    { range: "≥ 10,000,000 VNĐ", increment: "2,000,000 VNĐ" },
  ];

  return (
    <div className="container mx-auto p-4 md:p-8 bg-white shadow-lg rounded-lg">
      <h1 className="text-4xl font-extrabold text-center text-indigo-700 mb-10 flex items-center justify-center">
        <FaBookOpen className="mr-3 text-indigo-600" /> AucHub Auction Guide & Rules
      </h1>

      <p className={paragraphStyle}>
        Welcome to AucHub! To ensure a fair, transparent, and enjoyable experience for all our users, please familiarize yourself with the following rules and guidelines. Understanding these will help you navigate our platform and participate in auctions confidently.
      </p>

      {/* General Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaUsers className="mr-3 text-indigo-500" /> General Auction Rules & Information
        </h2>
        <p className={paragraphStyle}>
          These rules apply to all auctions conducted on AucHub, whether they are Live or Timed.
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Bids are Binding:</strong> Any bid you place is a binding contract. If you are the winning bidder, you are obligated to complete the purchase.
          </li>
          <li className={listItemStyle}>
            <strong>Item Condition:</strong> We encourage sellers to provide accurate descriptions and images. However, buyers are advised to carefully review all item details, descriptions, and photos before placing a bid. Ask questions if you are unsure.
          </li>
          <li className={listItemStyle}>
            <strong>Seller Bidding Prohibited:</strong> Sellers are not permitted to bid on their own auctions, either directly or indirectly.
          </li>
          <li className={listItemStyle}>
            <strong>Auction Cancellation:</strong> Sellers may cancel auctions only under specific circumstances (e.g., if the item is no longer available). Generally, only auctions that are currently 'Scheduled' or 'Active' (without bids, or with seller discretion if bids exist and platform allows) can be considered for cancellation.
          </li>
          <li className={listItemStyle}>
            <strong>User Conduct:</strong> All users are expected to act honestly and ethically. Any fraudulent activity, shill bidding, or manipulation of auctions will result in account suspension and other penalties.
          </li>
        </ul>
        <div className={placeholderImageStyle}>
          <span>Placeholder: General Auction Interface / Fair Play Graphic</span>
        </div>
      </section>

      {/* Bid Increments */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaDollarSign className="mr-3 text-indigo-500" /> Bid Increments
        </h2>
        <p className={paragraphStyle}>
          To maintain an orderly bidding process, all bids must adhere to the minimum bid increments set by the platform. The increment is based on the current highest bid. This table applies to both Live and Timed auctions:
        </p>
        <div className="overflow-x-auto shadow-md rounded-lg">
          <table className="min-w-full leading-normal">
            <thead className="bg-indigo-500 text-white">
              <tr>
                <th className="px-5 py-3 border-b-2 border-gray-200 text-left text-xs font-semibold uppercase tracking-wider">
                  Current Bid (VNĐ)
                </th>
                <th className="px-5 py-3 border-b-2 border-gray-200 text-left text-xs font-semibold uppercase tracking-wider">
                  Minimum Increment (VNĐ)
                </th>
              </tr>
            </thead>
            <tbody className="bg-white">
              {bidIncrementData.map((row, index) => (
                <tr key={index} className={index % 2 === 0 ? "bg-gray-50" : "bg-white"}>
                  <td className="px-5 py-4 border-b border-gray-200 text-sm">{row.range}</td>
                  <td className="px-5 py-4 border-b border-gray-200 text-sm">{row.increment}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Live Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
            <FaBolt className="mr-3 text-red-500" /> Live Auction Rules
        </h2>
        <p className={paragraphStyle}>
          Live Auctions are fast-paced, real-time bidding events. Here's what you need to know:
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Real-Time Bidding:</strong> Bids are placed and reflected instantly. You must be quick to respond to competing bids.
          </li>
          <li className={listItemStyle}>
            <strong>Placing a Bid:</strong> Your bid must be at least the current highest bid plus the minimum bid increment. If there are no bids, your bid must be at least the starting price.
          </li>
          <li className={listItemStyle}>
            <span className={liveAuctionPillStyle}>Soft-Close (Anti-Sniping)</span>
            If a bid is placed within the final <strong>60 seconds</strong> of a Live Auction, the auction closing time will be extended by an additional <strong>20 seconds</strong>. This process repeats if further bids are placed within the new extension period, ensuring everyone has a fair chance to bid.
          </li>
          <li className={listItemStyle}>
            <strong>Reserve Price:</strong> Some items may have a confidential minimum price (reserve) set by the seller. The item will not be sold unless the bidding meets or exceeds this reserve price. You will be notified if your bid meets the reserve.
          </li>
          <li className={listItemStyle}>
            <strong>Seller Hammer Down:</strong> Once a bid has been placed, and the reserve price (if any) has been met, the seller has the option to "hammer down" the auction at any time, immediately ending the auction and awarding the item to the current highest bidder.
          </li>
        </ul>
        <div className={placeholderImageStyle}>
          <span>Placeholder: Live Auction Bidding Screen / Hammer Icon</span>
        </div>
      </section>

      {/* Timed Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaClock className="mr-3 text-green-500" /> Timed Auction Rules
        </h2>
        <p className={paragraphStyle}>
          Timed Auctions run for a set duration, allowing you to place bids at any time before the auction closes. They typically use a proxy bidding system.
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Proxy Bidding (Automatic Bidding):</strong> When you place a bid, you enter the maximum amount you're willing to pay for the item. Our system will then automatically place bids on your behalf, using only enough of your maximum bid to keep you in the lead, up to your specified maximum.
            <ul className="list-disc ml-6 mt-2 space-y-1 text-sm">
                <li>Your maximum bid is kept confidential from other bidders and the seller.</li>
                <li>If another bidder places a bid, our system will automatically increase your bid by the minimum increment necessary to maintain your lead, as long as it doesn't exceed your maximum.</li>
                <li>If another bidder's maximum bid exceeds yours, you will be outbid and notified, giving you a chance to increase your maximum bid.</li>
            </ul>
          </li>
          <li className={listItemStyle}>
            <strong>Updating Your Max Bid:</strong> You can increase your maximum bid at any time before the auction ends. However, you generally cannot lower your maximum bid once placed.
          </li>
          <li className={listItemStyle}>
            <span className={timedAuctionPillStyle}>Soft-Close (Anti-Sniping)</span>
            Similar to Live Auctions, if a bid is placed (or if a proxy bid causes the lead to change) near the end of a Timed Auction (e.g., within the last few minutes, typically configured by the platform), the auction closing time may be extended. This gives other bidders a fair chance to respond. (The exact timing for extension may vary).
          </li>
          <li className={listItemStyle}>
            <strong>Reserve Price:</strong> Timed Auctions can also have reserve prices. If the highest bid at the auction's close does not meet the reserve, the item will not be sold.
          </li>
          <li className={listItemStyle}>
            <strong>Seller Ending Early:</strong> A seller may have the option to end a Timed Auction early if there are bids and they are satisfied with the current highest bid (especially if the reserve is met).
          </li>
           <li className={listItemStyle}>
            <strong>Auction Comments:</strong> You may be able to ask questions or leave comments on Timed Auction listings. Please keep comments respectful and relevant to the item.
          </li>
        </ul>
        <div className={placeholderImageStyle}>
          <span>Placeholder: Timed Auction Bid Input / Proxy Bidding Graphic</span>
        </div>
      </section>

      {/* Post-Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaShieldAlt className="mr-3 text-indigo-500" /> After Winning an Auction
        </h2>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Payment:</strong> If you are the winning bidder, you are expected to complete payment within <strong>48 hours</strong> of the auction closing. Payment instructions will be provided.
          </li>
          <li className={listItemStyle}>
            <strong>Shipping:</strong> Shipping costs are typically calculated based on your destination and the item's size/weight. These costs will be communicated to you after the auction. Ensure your shipping address is up to date in your profile.
          </li>
        </ul>
         <div className={placeholderImageStyle}>
          <span>Placeholder: Payment/Shipping Icons or Process Flow</span>
        </div>
      </section>

       <section className="my-8 text-center">
        <p className={paragraphStyle}>
          We hope this guide helps you understand how auctions work on AucHub. Our goal is to provide a vibrant and trustworthy marketplace.
        </p>
        <p className={paragraphStyle}>
          If you have any further questions, please don't hesitate to contact our support team or check our FAQ section (if available).
        </p>
      </section>
    </div>
  );
};

export default AuctionRulesGuidePage;