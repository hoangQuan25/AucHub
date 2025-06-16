import React, { useEffect, useState, useRef } from "react";
import SockJS from "sockjs-client/dist/sockjs";
import { Client } from "@stomp/stompjs";
import apiClient from "../api/apiClient";
import { useKeycloak } from "@react-keycloak/web";
import { FaPaperPlane } from "react-icons/fa"; // Using an icon for send button

const AuctionChatPanel = ({ auctionId }) => {
  const { keycloak, initialized } = useKeycloak();
  const [msgs, setMsgs] = useState([]);
  const [input, setInput] = useState("");
  const bottomRef = useRef(null);
  const stompRef = useRef(null);

  // Preload chat history
  useEffect(() => {
    if (auctionId) {
      apiClient
        .get(`/liveauctions/${auctionId}/chat?limit=100`)
        .then((res) => {
          setMsgs(res.data || []);
        })
        .catch((err) => {
          console.error("Failed to load chat history:", err);
        });
    }
  }, [auctionId]);

  // Connect STOMP
  useEffect(() => {
    if (!auctionId || !initialized || !keycloak.authenticated) return;

    const userId = keycloak.subject;

    const client = new Client({
      webSocketFactory: () => {
        const sockJsUrl = `${
          window.location.protocol
        }//localhost:8072/ws?uid=${encodeURIComponent(userId)}`;
        console.log("Attempting to connect SockJS to:", sockJsUrl); // For debugging
        return new SockJS(sockJsUrl);
      },
      connectHeaders: {
      },
      reconnectDelay: 5000,
      debug: (str) => {
        console.log("STOMP DEBUG: " + str);
      },
    });

    client.onConnect = (frame) => {
      console.log("STOMP Connected:", frame);
      stompRef.current = client; // Set stompRef here after successful connection
      client.subscribe(`/topic/chat.${auctionId}`, (m) => {
        try {
          const newMsg = JSON.parse(m.body);
          setMsgs((prevMsgs) => [...prevMsgs, newMsg]);
        } catch (e) {
          console.error("Error parsing incoming STOMP message:", e, m.body);
        }
      });
    };

    client.onStompError = (frame) => {
      console.error("STOMP Error:", frame.headers["message"]);
      console.error("Details:", frame.body);
    };

    client.onWebSocketError = (error) => {
      console.error("WebSocket Error:", error);
    };

    client.onWebSocketClose = (event) => {
      // Added event parameter for more details
      console.log("WebSocket Closed:", event);
    };

    client.activate();
    stompRef.current = client; // Store client in ref for cleanup

    return () => {
      if (stompRef.current && stompRef.current.active) {
        console.log("Deactivating STOMP client");
        stompRef.current.deactivate();
      }
    };
  }, [
    auctionId,
    initialized,
    keycloak.authenticated,
    keycloak.subject,
    keycloak.token,
  ]);

  // Auto-scroll
  useEffect(() => {
    if (bottomRef.current) {
      bottomRef.current.scrollIntoView({ behavior: "smooth" });
    }
  }, [msgs]);

  const send = () => {
    if (!input.trim() || !stompRef.current || !stompRef.current.active) {
      console.warn(
        "STOMP client not connected or active, or input empty. Cannot send message."
      );
      return;
    }
    const payload = { text: input.trim() };
    stompRef.current.publish({
      destination: `/app/chat.send.${auctionId}`,
      body: JSON.stringify(payload),
    });
    setInput("");
  };

  const loggedInUsername = keycloak.tokenParsed?.preferred_username;

  return (
    <div className="flex flex-col h-full overflow-hidden border border-gray-200 rounded-lg shadow">
      
      <div className="flex-1 overflow-y-auto p-3 text-sm bg-gray-50">
        {msgs.map((m, i) => {
          const isSelf = loggedInUsername === m.username;
          const isSeller = m.seller;
          const showAvatar = m.avatarUrl || m.username;

          return (
            <div
              key={m.id || i}
              className="flex items-center gap-2 py-1.5 px-1 group hover:bg-indigo-50 transition"
            >
              {/* Small avatar on the left */}
              {showAvatar && (
                <img
                  src={
                    m.avatarUrl ||
                    `https://ui-avatars.com/api/?name=${encodeURIComponent(
                      m.username || "A"
                    )}&background=random&size=32&font-size=0.5&length=1`
                  }
                  alt={m.username}
                  className="w-6 h-6 rounded-full object-cover flex-shrink-0"
                  title={m.username}
                />
              )}
              {/* Username and message inline */}
              <div className="flex-1 min-w-0">
                <span
                  className={`font-semibold mr-2 truncate ${
                    isSelf
                      ? "text-indigo-600"
                      : isSeller
                      ? "text-yellow-700"
                      : "text-gray-800"
                  }`}
                  title={m.username}
                >
                  {m.username}
                  {isSeller && (
                    <span className="ml-1 text-[10px] px-1.5 py-0.5 bg-yellow-300 text-yellow-900 rounded font-medium align-middle">
                      SELLER
                    </span>
                  )}
                </span>
                <span className="text-gray-800 break-words">{m.text}</span>
              </div>
              {/* Timestamp at the end, subtle */}
              <span className="ml-2 text-[11px] text-gray-400 flex-shrink-0">
                {new Date(m.timestamp).toLocaleTimeString([], {
                  hour: "numeric",
                  minute: "2-digit",
                  hour12: true,
                })}
              </span>
            </div>
          );
        })}
        <div ref={bottomRef}></div>
      </div>
      {/* Input field */}
      {keycloak.authenticated ? (
        <div className="border-t border-gray-200 p-3 flex items-center gap-2 bg-white">
          <input
            className="flex-1 border border-gray-300 rounded-full px-4 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-indigo-500 focus:border-indigo-500"
            value={input}
            maxLength={150} // Adjusted max length slightly
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && send()}
            placeholder="Type a message…"
          />
          <button
            onClick={send}
            className="p-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-full transition-colors duration-150 disabled:opacity-50 disabled:hover:bg-indigo-600"
            disabled={!input.trim() || !stompRef.current?.active}
            aria-label="Send message"
          >
            <FaPaperPlane size="1em" />
          </button>
        </div>
      ) : (
        <div className="border-t border-gray-200 p-3 text-center text-sm text-gray-500 bg-gray-50">
          Please{" "}
          <button
            onClick={() => keycloak.login()}
            className="text-indigo-600 hover:underline font-semibold"
          >
            log in
          </button>{" "}
          to chat.
        </div>
      )}
    </div>
  );
};

export default AuctionChatPanel;
