package mpeg2;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;

public class Main {

	public static void main(final String[] args) throws IOException, URISyntaxException {
		PacketDemux demux = new PacketDemux();
		ProgramMapTable programMap= new ProgramMapTable(demux);
		ProgramAssociationTable pat= new ProgramAssociationTable();
		pat.addAssociationTableListener(programMap);
		TableDemux tableZero= new TableDemux(0);
		tableZero.setTableConsumer(pat);
		TableDemux tableAtsc= new TableDemux(0x1FFB);
		demux.setProgramConsumer(tableZero);
		//demux.setProgramConsumer(tableAtsc);
		

		String path= "/home/mcmurtri/Downloads/ch2raw.ts";
		
		
		
		//HttpURLConnection con= (HttpURLConnection)(new URI("http://192.168.1.182:5004/auto/ch207000000").toURL().openConnection());
		
		try (BigEndianBitStreamInput in = new BigEndianBitStreamInput(new BufferedInputStream(/*con.getInputStream()*/ new FileInputStream(path)))) {

			MpegTsPacketHeader h = null;

			try {
				while (true) {
					h = (h == null) ? MpegTsPacketHeader.seekFrom(in) : MpegTsPacketHeader.from(in);
					System.out.println(h);
					
//					if (pids.add(h.pid())) {
//						demux.setProgramConsumer(h.pid(), new TableDemux(h.pid())); //Dumb testing
//					}
					
					demux.processPayload(h, in);
				}
			} catch (EOFException end) {
				throw end;
			} catch (IOException err) {
				err.printStackTrace();
				for (int i = 0; i < 100; ++i) {
					System.out.print(Integer.toHexString(in.readByte()));
				}
			}

			// TODO adaptation field ... optional fields ... optional fields
			// TODO payload
		}

	}

}
