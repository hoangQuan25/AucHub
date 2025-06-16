// src/components/CountdownTimer.jsx – MODIFIED again to handle days/months (approx)
import React, { useState, useEffect, useMemo } from "react";

function CountdownTimer({ endTimeMillis, onEnd }) { // Removed endedText again
  // Ensure endTimeMillis is treated as a number
  const validEndTime = Number(endTimeMillis);
  const isValidEndTime = !isNaN(validEndTime);

  // Calculate ms remaining, ensuring it's never negative
  const calcMsLeft = () => isValidEndTime ? Math.max(0, validEndTime - Date.now()) : 0;

  const [msLeft, setMsLeft] = useState(calcMsLeft);
  // Track if onEnd has been called for this specific endTimeMillis instance
  const [hasFiredEnd, setHasFiredEnd] = useState(msLeft <= 0);

  /* Reset whenever the endTimeMillis prop changes */
  useEffect(() => {
    const newMsLeft = calcMsLeft();
    setMsLeft(newMsLeft);
    setHasFiredEnd(newMsLeft <= 0); // Reset fired status on end time change
  }, [validEndTime]); // Depend only on the validated number

  /* Tick interval (using the user's 200ms interval) */
  useEffect(() => {
    // Check if already ended or invalid time provided
    if (msLeft <= 0 || !isValidEndTime) {
      // Fire onEnd only if it hasn't fired *for this endTime* instance yet
      if (!hasFiredEnd && isValidEndTime && msLeft <= 0) {
        onEnd?.();
        setHasFiredEnd(true); // Mark as fired
      }
      return; // No interval needed
    }

    // Interval logic
    const id = setInterval(() => {
        const newMsLeft = calcMsLeft();
        setMsLeft(newMsLeft); // Update state

        // Check if ended *inside* interval callback
        if (newMsLeft <= 0 && !hasFiredEnd) {
             onEnd?.();
             setHasFiredEnd(true);
        }
    }, 200); // User's preferred 200ms interval

    // Cleanup interval
    return () => clearInterval(id);
  }, [msLeft, validEndTime, onEnd, hasFiredEnd]); // Dependencies


  // --- Formatting Logic using useMemo ---
  const formattedTimeLeft = useMemo(() => {
    // Handle invalid end time gracefully
     if (!isValidEndTime) {
        return "--:--"; // Placeholder for invalid input
     }

    // Always calculate based on msLeft, even if 0
    const totalSeconds = Math.floor(msLeft / 1000);
    // Use approximate average days per month for display (e.g., 30.44) - adjust if needed
    const approxDaysPerMonth = 30.4375;
    const totalDays = totalSeconds / (3600 * 24);
    const months = Math.floor(totalDays / approxDaysPerMonth);
    // Calculate remaining days *after* taking out full months
    const days = Math.floor(totalDays % approxDaysPerMonth);
    // Calculate hours based on seconds remaining *after* taking out full days
    const hours = Math.floor((totalSeconds % (3600 * 24)) / 3600);
    // Calculate minutes based on seconds remaining *after* taking out full hours
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    // Calculate remaining seconds
    const seconds = totalSeconds % 60;

    // --- Conditional Formatting ---
    if (msLeft <= 0) {
        return "00:00"; // Display 00:00 when ended
    } else if (months >= 1) {
        // Display months and remaining days (approximate)
        let parts = [`${months}mo`];
        if (days > 0) {
             parts.push(`${days}d`);
        }
        return parts.join(' ') + ' left';
    } else if (days >= 1) {
        // Display days, hours, and optionally minutes
        let parts = [`${days}d`];
         if (hours > 0) {
             parts.push(`${hours}h`);
         }
         // Add minutes if less than, say, 3 days? Or always? Let's add if > 0.
         if (minutes > 0) {
             parts.push(`${minutes}m`);
         }
        return parts.join(' ') + ' left';
    } else if (hours >= 1) {
        // Less than a day, more than an hour: HH:MM:SS format
        return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    } else {
        // Less than an hour: MM:SS format
        return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    }

  }, [msLeft, isValidEndTime]); // Recalculate only when msLeft changes


  // Determine styling - Use user's original critical threshold (20 seconds)
  const totalSec = Math.floor(msLeft / 1000);
  // Apply critical style only if time is left AND <= 20 seconds
  const critical = isValidEndTime && totalSec > 0 && totalSec <= 20;

  return (
    // Keep user's original styling structure
    <span className={`font-bold text-xl ${critical ? "text-red-600 animate-pulse" : "text-gray-800"}`}>
      {formattedTimeLeft}
    </span>
  );
}

export default CountdownTimer;