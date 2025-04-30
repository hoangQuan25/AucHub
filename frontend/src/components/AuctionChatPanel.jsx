import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client/dist/sockjs";
import { Client } from "@stomp/stompjs";
import apiClient from "../api/apiClient";
import { useKeycloak } from "@react-keycloak/web";

const AuctionChatPanel = ({ auctionId }) => {
  const { keycloak } = useKeycloak();
  const [msgs, setMsgs] = useState([]);
  const [input, setInput] = useState("");
  const bottomRef = useRef(null);
  const stompRef = useRef(null);
  const lastHistoryTimestampRef = useRef(0);

  /* preload last 100 */
  useEffect(() => {
    apiClient.get(`/liveauctions/${auctionId}/chat?limit=100`).then((res) => {
      const historyMsgs = res.data || [];
      setMsgs(historyMsgs);
      // --- NEW: Store the latest timestamp ---
      if (historyMsgs.length > 0) {
        // Assuming timestamps are comparable numbers (like epoch ms)
        // Or convert dates to comparable numbers if necessary
        const latestTimestamp = new Date(
          historyMsgs[historyMsgs.length - 1].timestamp
        ).getTime();
        lastHistoryTimestampRef.current = latestTimestamp;
        console.log("Last history timestamp:", latestTimestamp); // For debugging
      } else {
        lastHistoryTimestampRef.current = 0; // Reset if history is empty
      }
      // --- End NEW ---
    });
  }, [auctionId]);

  /* connect STOMP */
  useEffect(() => {
    if (!keycloak.authenticated) return;
    const userId = keycloak.tokenParsed.sub;

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(
          `${
            window.location.protocol
          }//localhost:8072/ws?uid=${encodeURIComponent(userId)}`
        ),
      reconnectDelay: 5000,
    });
    client.onConnect = () => {
      client.subscribe(`/topic/chat.${auctionId}`, (m) => {
        setMsgs((prev) => [...prev, JSON.parse(m.body)]);
      });
    };
    client.activate();
    stompRef.current = client;
    return () => client.deactivate();
  }, [auctionId, keycloak.authenticated]);

  /* auto-scroll */
  useEffect(
    () => bottomRef.current?.scrollIntoView({ behavior: "smooth" }),
    [msgs]
  );

  const send = () => {
    if (!input.trim()) return;
    const payload = {
      text: input.trim(),
    };
    stompRef.current.publish({
      destination: `/app/chat.send.${auctionId}`,
      body: JSON.stringify(payload),
    });
    setInput("");
  };

  return (
    <div className="flex flex-col h-full border rounded shadow bg-white">
      {/* Chat messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2 text-sm bg-gray-50">
        {msgs.map((m, i) => {
          const isSelf =
            keycloak.tokenParsed?.preferred_username === m.username;
          const isSeller = m.seller;

          return (
            <div
              key={i}
              className={`flex ${isSelf ? "justify-end" : "justify-start"}`}
            >
              <div
                className={`max-w-xs px-3 py-2 rounded-lg shadow-sm ${
                  isSelf
                    ? "bg-indigo-100 text-indigo-900"
                    : isSeller
                    ? "bg-yellow-50 border border-yellow-300 text-yellow-900"
                    : "bg-white text-gray-800 border"
                }`}
              >
                <div className="font-semibold text-xs mb-0.5">
                  {m.username}
                  {isSeller && (
                    <span className="ml-1 text-[10px] px-1.5 py-0.5 bg-yellow-200 text-yellow-800 rounded-full">
                      seller
                    </span>
                  )}
                </div>
                <div className="break-words whitespace-pre-wrap">{m.text}</div>
                <div className="text-[10px] text-right text-gray-400 mt-1">
                  {new Date(m.timestamp).toLocaleTimeString()}
                </div>
              </div>
            </div>
          );
        })}

        <div ref={bottomRef}></div>
      </div>

      {/* Input field */}
      <div className="border-t p-3 flex items-center gap-2 bg-white">
        <input
          className="flex-1 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring focus:border-indigo-500"
          value={input}
          maxLength={140}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
          placeholder="Type a message…"
        />
        <button
          onClick={send}
          className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm transition disabled:opacity-50"
          disabled={!input.trim()}
        >
          Send
        </button>
      </div>
    </div>
  );
};

export default AuctionChatPanel;
