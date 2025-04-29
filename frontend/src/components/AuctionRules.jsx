import React from "react";
import CollapsibleSection from "./CollapsibleSection";

const increments = [
  ["< 50 000", "5 000"],
  ["50 000 – 99 999", "10 000"],
  ["100 000 – 199 999", "20 000"],
  ["200 000 – 499 999", "50 000"],
  ["500 000 – 999 999", "100 000"],
  ["1 000 000 – 1 999 999", "200 000"],
  ["2 000 000 – 4 999 999", "500 000"],
  ["5 000 000 – 9 999 999", "1 000 000"],
  ["≥ 10 000 000", "2 000 000"],
];

const AuctionRules = () => (
  <CollapsibleSection title="Payment, Shipping & Auction Rules">
    <div className="space-y-4 text-sm text-gray-700 leading-relaxed">
      {/* Rules */}
      <ul className="list-disc pl-5 space-y-2">
        <li>All bids are binding. Check item condition before bidding.</li>
        <li>
          <span className="font-medium">“Soft-close”:</span> a bid in the final 60 s extends the auction by 20 s.
        </li>
        <li>
          Seller may hammer down at any time once a bid has been placed and
          (if set) the reserve is met.
        </li>
        <li>
          Buyer pays within 48 h. Shipping cost based on destination.
        </li>
      </ul>

      {/* Table */}
      <div>
        <h5 className="font-semibold text-gray-800 mb-2">
          Minimum-bid increments
        </h5>
        <div className="overflow-x-auto rounded border border-gray-200">
          <table className="w-full text-xs text-left border-collapse">
            <thead className="bg-gray-50 text-gray-600 uppercase tracking-wider text-[11px]">
              <tr>
                <th className="p-3 border-b border-gray-200">Current bid (VNĐ)</th>
                <th className="p-3 border-b border-gray-200">Min. increment</th>
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
