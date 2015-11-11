package ricartAgrawala;
// Visidia imports
import visidia.simulation.process.messages.Door;
import commun.*;

// Reception thread
public class ReceptionRules extends Thread {

	RicartAgrawalaMutualExclusion algo;

	public ReceptionRules( RicartAgrawalaMutualExclusion a ) {

		algo = a;
	}

	public void run() {

		Door d = new Door();

		while( true ) {

			SyncMessage m = (SyncMessage) algo.recoit(d);
			int door = d.getNum();

			switch (m.getMsgType()) {

				case REQ :
					algo.receiveREQ( m.getMsgProc(),m.getMsgProcTarget(), m.getMsgHorloge(), door );
					break;
				case REL :
					algo.receiveREL(m.getMsgProc(), m.getMsgProcTarget(), door );
					break;
				case FORME :
					algo.receiveFORME( m.getMsgForme(), m.getMsgProc(), m.getMsgProcTarget(), door );
					break;
				case REGISTER :
					algo.receiveREGISTER( m.getMsgProc(), door );
					break;
				case END_REGISTER :
					algo.receiveEND_REGISTER( m.getMsgProc(), m.getMsgProcTarget(), door );
					break;


				default:
					System.out.println("Error message type");
			}
		}
	}
}

