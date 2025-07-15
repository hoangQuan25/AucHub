import React from "react";
import CollapsibleSection from "./CollapsibleSection";

const increments = [
  ["< 50,000", "500"],
  ["50,000 – 99,999", "1,000"],
  ["100,000 – 299,999", "5,000"],
  ["300,000 – 999,999", "10,000"],
  ["1,000,000 – 2,999,999", "50,000"],
  ["3,000,000 – 4,999,999", "100,000"],
  ["5,000,000 – 9,999,999", "200,000"],
  ["10,000,000 – 19,999,999", "500,000"],
  ["20,000,000 – 49,999,999", "1,000,000"],
  ["≥ 50,000,000", "2,000,000"],
];

const AuctionRules = () => (
  <CollapsibleSection title="Payment, Shipping & Auction Rules">
    <div className="space-y-4 text-sm text-gray-700 leading-relaxed">
      {/* Rules */}
      <ul className="list-disc pl-5 space-y-2">
        <li>
          <span className="font-medium">Bids are Binding:</span> Any bid you place is a binding contract. If you are the winning bidder, you are obligated to complete the purchase.
        </li>
        <li>
          <span className="font-medium">Item Condition:</span> Sellers should provide accurate descriptions and images. Buyers are advised to carefully review all item details before bidding.
        </li>
        <li>
          <span className="font-medium">Seller Bidding Prohibited:</span> Sellers are not permitted to bid on their own auctions, either directly or indirectly. Any such activity will result in account suspension.
        </li>
        <li>
          <span className="font-medium">User Conduct:</span> All users are expected to act honestly and ethically. Any fraudulent activity, shill bidding, or manipulation of auctions will result in penalties.
        </li>
        <li>
          <span className="font-medium">Soft-close (Anti-Sniping):</span> If a bid is placed within the final <strong>60 seconds</strong> of a Live Auction, the closing time will be extended by an additional <strong>20 seconds</strong>. For Timed Auctions, a bid in the final <strong>10 minutes</strong> extends the auction by <strong>5 minutes</strong>.
        </li>
        <li>
          <span className="font-medium">Reserve Price:</span> Some items may have a confidential minimum price (reserve) set by the seller. The item will not be sold unless the bidding meets or exceeds this price.
        </li>
        <li>
          <span className="font-medium">Seller Hammer Down:</span> Once the reserve price (if any) has been met, the seller has the option to "hammer down" the auction at any time, immediately ending the auction and awarding the item to the current highest bidder.
        </li>
        <li>
          <span className="font-medium">Buyer Payment:</span> Winners must complete payment within <strong>48 hours</strong> of the auction ending. Shipping cost is based on destination.
        </li>
      </ul>

      {/* Table */}
      <div>
        <h5 className="font-semibold text-gray-800 mb-2">
          Minimum-bid increments
        </h5>
        <div className="overflow-x-auto rounded border border-gray-200">
          <table className="min-w-[500px] w-full text-xs text-left border-collapse">
            <thead className="bg-gray-50 text-gray-600 uppercase tracking-wider text-[11px]">
              <tr>
                <th className="p-3 border-b border-gray-200">Current Bid (VNĐ)</th>
                <th className="p-3 border-b border-gray-200">Min. Increment (VNĐ)</th>
              </tr>
            </thead>
            <tbody>
              {increments.map(([range, inc]) => (
                <tr key={range} className="hover:bg-gray-50 transition">
                  <td className="p-3 border-t border-gray-100">{range}</td>
                  <td className="p-3 border-t border-gray-100">{inc}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </CollapsibleSection>
);

export default AuctionRules;