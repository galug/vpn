import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VinMappingServerBefore {

	private static final int SERVER_PORT = 8080;
	private static final Logger LOGGER = Logger.getLogger(VinMappingServer.class.getName());
	private static final Map<String, String> mappingTable = new ConcurrentHashMap<>();

	public static void main(String[] args) {
		try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
			LOGGER.info("Server is running on port " + SERVER_PORT);

			while (true) {
				byte[] buffer = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				serverSocket.receive(packet);

				// Handle each request in a new thread
				new Thread(() -> handleClientRequest(packet, serverSocket)).start();
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

			if (!clientMessage.startsWith("VIN:")) {
				sendResponse("Invalid message format. Expected: VIN:<17-character VIN>", clientAddress, clientPort,
					serverSocket);
				return;
			}

			String vinNumber = clientMessage.substring(4).trim();
			if (!isValidVin(vinNumber)) {
				sendResponse("Invalid VIN number: " + vinNumber, clientAddress, clientPort, serverSocket);
			}

			mappingTable.put(clientAddress.toString(), vinNumber);
			String response = "Mapping registered: IP = " + clientAddress + ", VIN = " + vinNumber;
			sendResponse(response, clientAddress, clientPort, serverSocket);
			LOGGER.info(response);
			LOGGER.info(mappingTable.toString());
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
