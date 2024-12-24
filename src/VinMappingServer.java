import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VinMappingServer {

	private static final int SERVER_PORT = 8080;
	private static final Logger LOGGER = Logger.getLogger(VinMappingServer.class.getName());
	private static final Map<String, String> mappingTable = new ConcurrentHashMap<>();
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);

	public static void main(String[] args) {
		try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
			LOGGER.info("Server is running on port " + SERVER_PORT);

			while (true) {
				byte[] buffer = new byte[64];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				serverSocket.receive(packet);

				threadPool.execute(() -> handleClientRequest(packet, serverSocket));
			}
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Server error: ", e);
		}
	}

	private static void handleClientRequest(DatagramPacket packet, DatagramSocket serverSocket) {
		try {
			String clientMessage = new String(packet.getData(), 0, packet.getLength()).trim();
			InetAddress clientAddress = packet.getAddress();
			int clientPort = packet.getPort();

			LOGGER.info("Received message: " + clientMessage + " from " + clientAddress);

			if (!clientMessage.startsWith("VIN:") || !clientMessage.contains(",IP:")) {
				sendResponse("Invalid message format. Expected: VIN:<17-character VIN>,IP:<IP Address>", clientAddress, clientPort, serverSocket);
				return;
			}

			// Parse VIN and IP from the message
			String[] parts = clientMessage.split(",IP:");
			if (parts.length != 2) {
				sendResponse("Invalid message format. Expected: VIN:<17-character VIN>,IP:<IP Address>", clientAddress, clientPort, serverSocket);
				return;
			}

			String vinNumber = parts[0].substring(4).trim();
			String address = parts[1].trim();

			if (!isValidVin(vinNumber)) {
				sendResponse("Invalid VIN number: " + vinNumber, clientAddress, clientPort, serverSocket);
				return;
			}

			// String clientKey = clientAddress.toString() + ":" + clientPort;
			mappingTable.put(address, vinNumber);
			sendResponse("Mapping registered.", clientAddress, clientPort, serverSocket);
			LOGGER.info("Mapping registered for IP = " + address);
			LOGGER.info("Mapping Table = " + mappingTable);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error handling client request: ", e);
		}
	}

	private static boolean isValidVin(String vin) {
		return vin != null && vin.matches("[A-HJ-NPR-Z0-9]{17}");
	}

	private static void sendResponse(String response, InetAddress clientAddress, int clientPort,
		DatagramSocket serverSocket) {
		try {
			byte[] responseBytes = response.getBytes();
			DatagramPacket responsePacket = new DatagramPacket(responseBytes, responseBytes.length, clientAddress,
				clientPort);
			serverSocket.send(responsePacket);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error sending response to client: ", e);
		}
	}
}
