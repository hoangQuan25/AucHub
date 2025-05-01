// src/components/CountdownTimer.jsx – improved: reacts to end‑time changes and keeps clients in‑sync
import React, { useState, useEffect } from "react";

/**
 * CountdownTimer
 * @param {number} endTimeMillis – absolute epoch‑ms when the countdown should reach 0
 * @param {function} onEnd – optional callback fired exactly once when 0 is reached
 */
function CountdownTimer({ endTimeMillis, onEnd }) {
  const calcMsLeft = () => Math.max(0, endTimeMillis - Date.now());

  const [msLeft, setMsLeft] = useState(calcMsLeft);
  const [hasFiredEnd, setHasFiredEnd] = useState(false);

  /* Reset whenever the prop changes */
  useEffect(() => {
    setMsLeft(calcMsLeft());
    setHasFiredEnd(false);
  }, [endTimeMillis]);

  /* Tick every 200 ms for smoother sync across browsers */
  useEffect(() => {
    if (msLeft === 0) {
      if (!hasFiredEnd) {
        onEnd?.();
        setHasFiredEnd(true);
      }
      return; // no interval needed once finished
    }
    const id = setInterval(() => setMsLeft(calcMsLeft()), 200);
    return () => clearInterval(id);
  }, [msLeft, endTimeMillis, onEnd, hasFiredEnd]);

  const totalSec = Math.floor(msLeft / 1000);
  const minutes = String(Math.floor(totalSec / 60)).padStart(2, "0");
  const seconds = String(totalSec % 60).padStart(2, "0");
  const critical = totalSec <= 20;

  return (
    <span className={`font-bold text-xl ${critical ? "text-red-600 animate-pulse" : "text-gray-800"}`}>
      {minutes}:{seconds}
    </span>
  );
}

export default CountdownTimer;