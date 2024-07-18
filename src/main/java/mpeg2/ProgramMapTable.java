package mpeg2;

import mpeg2.ProgramAssociationTable.AssociationTable;
import mpeg2.ProgramAssociationTable.AssociationTableListener;

public class ProgramMapTable implements AssociationTableListener {
	private final PacketDemux deumx;
	
	public ProgramMapTable(PacketDemux deumx) {
		this.deumx = deumx;
	}

	@Override
	public void newAssociationTable(AssociationTable table) {
		// TODO register demux

	}

}
