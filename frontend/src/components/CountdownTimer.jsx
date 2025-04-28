import React, { useState, useEffect, useRef, useCallback } from 'react';

// --- Simple Countdown Timer Component ---
const CountdownTimer = ({ endTimeMillis, onEnd }) => {
  const calculateTimeLeft = useCallback(() => {
    const now = Date.now();
    const difference = endTimeMillis - now;
    let timeLeft = {};

    if (difference > 0) {
      timeLeft = {
        // total: difference, // Optional: total milliseconds
        minutes: Math.floor((difference / 1000 / 60) % 60),
        seconds: Math.floor((difference / 1000) % 60),
      };
    } else {
      timeLeft = { minutes: 0, seconds: 0 };
    }
    return timeLeft;
  }, [endTimeMillis]);

  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());
  const [isEnded, setIsEnded] = useState(false);

  useEffect(() => {
    // Exit early if already ended
    if (isEnded) return;
    // Reset ended state if end time changes
    setIsEnded(Date.now() >= endTimeMillis);

    const timer = setTimeout(() => {
      const newTimeLeft = calculateTimeLeft();
      setTimeLeft(newTimeLeft);
      if (newTimeLeft.minutes === 0 && newTimeLeft.seconds === 0) {
        console.log("Countdown Timer Ended.");
        setIsEnded(true);
        if (onEnd) onEnd(); // Call callback if provided
      }
    }, 1000); // Update every second

    // Cleanup timeout on unmount or when endTimeMillis changes
    return () => clearTimeout(timer);
  }, [timeLeft, endTimeMillis, calculateTimeLeft, isEnded, onEnd]); // Rerun effect when timeLeft or endTimeMillis changes

  const displayMinutes = String(timeLeft.minutes || 0).padStart(2, "0");
  const displaySeconds = String(timeLeft.seconds || 0).padStart(2, "0");
  const critical = timeLeft.minutes === 0 && (timeLeft.seconds || 0) <= 20; // Example: critical under 20s

  return (
    <span
      className={`font-bold text-xl ${
        critical ? "text-red-600 animate-pulse" : "text-gray-800"
      }`}
    >
      {displayMinutes}:{displaySeconds}
    </span>
  );
};
// --- End Countdown Timer Component ---

export default CountdownTimer;