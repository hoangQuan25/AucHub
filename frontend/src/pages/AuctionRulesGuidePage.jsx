import React from "react";
import {
  FaBookOpen,
  FaGavel,
  FaClock,
  FaUsers,
  FaShieldAlt,
  FaBolt,
  FaDollarSign,
  FaUndo,
  FaShippingFast,
  FaCheckCircle,
  FaUserSlash,
} from "react-icons/fa";

const AuctionRulesGuidePage = () => {
  const commonRulesPillStyle =
    "bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium inline-block mr-2 mb-2";
  const liveAuctionPillStyle =
    "bg-red-100 text-red-800 px-3 py-1 rounded-full text-sm font-medium inline-block mr-2 mb-2";
  const timedAuctionPillStyle =
    "bg-green-100 text-green-800 px-3 py-1 rounded-full text-sm font-medium inline-block mr-2 mb-2";
  const sectionTitleStyle =
    "text-3xl font-bold text-gray-800 mb-6 pb-2 border-b-2 border-indigo-500 flex items-center";
  const subTitleStyle =
    "text-2xl font-semibold text-gray-700 mb-4 mt-8 flex items-center";
  const paragraphStyle = "text-gray-700 leading-relaxed mb-4";
  const listItemStyle = "ml-5 list-disc text-gray-700 mb-2";
  const placeholderImageStyle =
    "w-full h-48 bg-gray-200 border border-gray-300 rounded-lg flex items-center justify-center text-gray-500 text-lg font-semibold my-6 shadow-inner italic";

  const bidIncrementData = [
    { range: "< 50,000 VNĐ", increment: "500 VNĐ" },
    { range: "50,000 – 99,999 VNĐ", increment: "1,000 VNĐ" },
    { range: "100,000 – 299,999 VNĐ", increment: "5,000 VNĐ" },
    { range: "300,000 – 999,999 VNĐ", increment: "10,000 VNĐ" },
    { range: "1,000,000 – 2,999,999 VNĐ", increment: "50,000 VNĐ" },
    { range: "3,000,000 – 4,999,999 VNĐ", increment: "100,000 VNĐ" },
    { range: "5,000,000 – 9,999,999 VNĐ", increment: "200,000 VNĐ" },
    { range: "10,000,000 – 19,999,999 VNĐ", increment: "500,000 VNĐ" },
    { range: "20,000,000 – 49,999,999 VNĐ", increment: "1,000,000 VNĐ" },
    { range: "≥ 50,000,000 VNĐ", increment: "2,000,000 VNĐ" },
  ];

  return (
    <div className="container mx-auto p-4 md:p-8 bg-white shadow-lg rounded-lg">
      <h1 className="text-4xl font-extrabold text-center text-indigo-700 mb-10 flex items-center justify-center">
        <FaBookOpen className="mr-3 text-indigo-600" /> AucHub Auction Guide &
        Rules
      </h1>

      <p className={paragraphStyle}>
        Welcome to AucHub! To ensure a fair, transparent, and enjoyable
        experience for all our users, please familiarize yourself with the
        following rules and guidelines. Understanding these will help you
        navigate our platform and participate in auctions confidently.
      </p>

      {/* General Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaUsers className="mr-3 text-indigo-500" /> General Auction Rules
        </h2>
        <p className={paragraphStyle}>
          These rules apply to all auctions conducted on AucHub, whether they
          are Live or Timed.
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Bids are Binding:</strong> Any bid you place is a binding
            contract. If you are the winning bidder, you are obligated to
            complete the purchase.
          </li>
          <li className={listItemStyle}>
            <strong>Item Condition:</strong> We encourage sellers to provide
            accurate descriptions and images. However, buyers are advised to
            carefully review all item details before placing a bid.
          </li>
          <li className={listItemStyle}>
            <strong>Seller Bidding Prohibited:</strong> Sellers are not
            permitted to bid on their own auctions, either directly or
            indirectly. Any such activity will result in account suspension.
          </li>
          <li className={listItemStyle}>
            <strong>User Conduct:</strong> All users are expected to act
            honestly and ethically. Any fraudulent activity, shill bidding, or
            manipulation of auctions will result in penalties.
          </li>
        </ul>
      </section>

      {/* Bid Increments */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaDollarSign className="mr-3 text-indigo-500" /> Bid Increments
        </h2>
        <p className={paragraphStyle}>
          To maintain an orderly bidding process, all bids must adhere to the
          minimum bid increments set by the platform. The increment is based on
          the current highest bid.
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
                <tr
                  key={index}
                  className={index % 2 === 0 ? "bg-gray-50" : "bg-white"}
                >
                  <td className="px-5 py-4 border-b border-gray-200 text-sm">
                    {row.range}
                  </td>
                  <td className="px-5 py-4 border-b border-gray-200 text-sm font-medium">
                    {row.increment}
                  </td>
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
          Live Auctions are fast-paced, real-time bidding events. Here's what
          you need to know:
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Real-Time Bidding:</strong> Bids are placed and reflected
            instantly. You must be quick to respond to competing bids.
          </li>
          <li className={listItemStyle}>
            <span className={liveAuctionPillStyle}>
              Soft-Close (Anti-Sniping)
            </span>
            If a bid is placed within the final <strong>60 seconds</strong> of a
            Live Auction, the closing time will be extended by an additional{" "}
            <strong>20 seconds</strong>. This prevents "sniping" and ensures
            everyone has a fair chance to bid.
          </li>
          <li className={listItemStyle}>
            <strong>Reserve Price:</strong> Some items may have a confidential
            minimum price (reserve) set by the seller. The item will not be sold
            unless the bidding meets or exceeds this price.
          </li>
          <li className={listItemStyle}>
            <strong>Seller Hammer Down:</strong> Once the reserve price (if any)
            has been met, the seller has the option to "hammer down" the auction
            at any time, immediately ending the auction and awarding the item to
            the current highest bidder.
          </li>
        </ul>
        <div className="w-full flex items-center justify-center my-6">
          <img
            src="/live.png"
            alt="Screenshot of the Live Auction bidding interface"
            className="w-full max-w-1xl rounded-lg border border-gray-300 shadow-inner object-contain"
          />
        </div>
      </section>

      {/* Timed Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaClock className="mr-3 text-green-500" /> Timed Auction Rules
        </h2>
        <p className={paragraphStyle}>
          Timed Auctions run for a set duration, allowing you to place bids at
          any time before the auction closes.
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Proxy Bidding (Automatic Bidding):</strong> You enter the
            maximum amount you're willing to pay. Our system then automatically
            bids on your behalf, using only enough to keep you in the lead, up
            to your maximum. Your maximum bid is kept confidential.
          </li>
          <li className={listItemStyle}>
            <span className={timedAuctionPillStyle}>
              Soft-Close (Anti-Sniping)
            </span>
            {/* <<< RULE UPDATED HERE */}
            If a bid is placed within the final <strong>10 minutes</strong> of a
            Timed Auction, the auction's closing time will be extended by an
            additional <strong>5 minutes</strong> from the time of the bid. This
            ensures all bidders have a fair opportunity to respond.
          </li>
        </ul>
        <div className="w-full flex items-center justify-center my-6">
          <img
            src="/timed.png"
            alt="Screenshot of the Timed Auction bidding interface"
            className="w-full max-w-1xl rounded-lg border border-gray-300 shadow-inner object-contain"
          />
        </div>
      </section>

      {/* <<< NEW SECTION FOR BAN MECHANIC */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaUserSlash className="mr-3 text-yellow-600" /> Penalties for
          Non-Payment
        </h2>
        <p className={paragraphStyle}>
          To maintain a trustworthy marketplace for our sellers, we enforce a
          strict policy regarding payment defaults. A payment default occurs
          when a winning bidder fails to pay for their item within the 48-hour
          deadline.
        </p>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Three-Strike Rule:</strong> Upon the{" "}
            <strong>third (3rd)</strong> payment default, a user's account will
            be automatically suspended from all bidding and listing activities
            for <strong>1 week</strong>.
          </li>
          <li className={listItemStyle}>
            <strong>Ban Escalation:</strong> Any subsequent payment default
            after a ban has been served will result in a longer suspension
            period, beginning with a <strong>1-month ban</strong>.
          </li>
          <li className={listItemStyle}>
            <strong>Effect of Ban:</strong> While an account is banned, you will
            not be able to place bids or create new auctions. You will still
            have access to your account to manage existing orders and
            deliveries.
          </li>
        </ul>
        <div className="w-full flex items-center justify-center my-6">
          <img
            src="/ban.png"
            alt="Screenshot of the ban mechanic interface"
            className="w-full max-w-1xl rounded-lg border border-gray-300 shadow-inner object-contain"
          />
        </div>
      </section>

      {/* Post-Auction Rules */}
      <section className="my-12">
        <h2 className={sectionTitleStyle}>
          <FaShieldAlt className="mr-3 text-indigo-500" /> After the Auction
          Ends
        </h2>
        <p className={paragraphStyle}>
          Congratulations on your winning bid! Here's what happens next. This
          process ensures security for both buyers and sellers.
        </p>

        <h3 className={subTitleStyle}>
          <FaDollarSign className="mr-3 text-green-600" />
          1. Payment
        </h3>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Payment Deadline:</strong> Winners must complete payment
            from the Order Details page within <strong>48 hours</strong> of the
            auction ending.
          </li>
          <li className={listItemStyle}>
            <strong>Failure to Pay:</strong> If payment is not received within
            48 hours, the order is cancelled for the winner. This counts as a
            "payment default" and may lead to penalties (see above). The item
            then moves to the "Second Chance" process.
          </li>
        </ul>

        <h3 className={subTitleStyle}>
          <FaUsers className="mr-3 text-blue-600" />
          2. The Second Chance Process
        </h3>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            When a winner fails to pay, the seller is notified and can choose
            one of two options:
            <ul className="list-disc ml-6 mt-2 space-y-1 text-sm">
              <li>
                <strong>Offer to Next Bidder:</strong> The seller can offer the
                item to the next highest eligible bidder at their final bid
                price. This bidder will receive a notification and has 48 hours
                to accept and pay.
              </li>
              <li>
                <strong>Re-list the Item:</strong> The seller can choose to
                start a completely new auction for the item.
              </li>
            </ul>
          </li>
        </ul>

        <h3 className={subTitleStyle}>
          <FaShippingFast className="mr-3 text-orange-600" />
          3. Shipping & Delivery
        </h3>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>Seller Ships:</strong> Once payment is confirmed, the seller
            is notified to ship the item. They will provide a carrier and
            tracking number, which you can see on the Order Details page.
          </li>
          <li className={listItemStyle}>
            <strong>Track Your Item:</strong> You can follow the estimated
            delivery countdown on the order page.
          </li>
        </ul>

        <h3 className={subTitleStyle}>
          <FaCheckCircle className="mr-3 text-teal-600" />
          4. Confirmation & Buyer Protection
        </h3>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            <strong>The 7-Day Window:</strong> When the item is marked as
            delivered, a <strong>7-day buyer protection window</strong> begins.
          </li>
          <li className={listItemStyle}>
            During this week, you must visit the Order Details page to either:
            <ul className="list-disc ml-6 mt-2 space-y-1 text-sm">
              <li>
                Click <strong>"Confirm Receipt"</strong> if the item is as
                described. This completes the order and releases payment to the
                seller.
              </li>
              <li>
                Click <strong>"Request Return"</strong> if the item has issues.
              </li>
            </ul>
          </li>
          <li className={listItemStyle}>
            <strong>Automatic Completion:</strong> If you take no action within
            7 days, the order will be automatically completed.
          </li>
        </ul>

        <h3 className={subTitleStyle}>
          <FaUndo className="mr-3 text-red-600" />
          5. Returns & Refunds
        </h3>
        <ul className="space-y-3">
          <li className={listItemStyle}>
            If you request a return, you must state a reason and provide
            details. The seller then has to approve the return.
          </li>
          <li className="listItemStyle">
            A refund is only issued to your original payment method after the
            seller confirms they have received the returned item.
          </li>
        </ul>

        {/* <div className={placeholderImageStyle}>
          <span>
            Flowchart of the entire post-auction process: Win - Pay - Ship -
            Confirm/Return.
          </span>
        </div> */}
      </section>

      <section className="my-8 text-center">
        <p className={paragraphStyle}>
          We hope this guide helps you understand how auctions work on AucHub.
          Our goal is to provide a vibrant and trustworthy marketplace for
          everyone.
        </p>
      </section>
    </div>
  );
};

export default AuctionRulesGuidePage;
