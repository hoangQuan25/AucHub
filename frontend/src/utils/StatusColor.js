export default function getStatusColor(status) {
  switch (status) {
    case "ACTIVE":
      return "text-green-500";
    case "ENDED":
      return "text-yellow-500";
    case "CANCELED":
      return "text-red-500";
    default:
      return "text-gray-500";
  }
}