package workcraft.unfolding;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Prefix {
	HashMap <Event, Event> cutoffs = new HashMap<Event, Event>();
	ArrayList<Event> events;
	ArrayList<Condition> conditions;
	ArrayList<String> tIds = new ArrayList<String>();
	ArrayList<String> pIds = new ArrayList<String>();
	
	public void fromMCI(File f) throws IOException {
		FileInputStream input = new FileInputStream (f);
		DataInputStream data    = new DataInputStream (input );
		
		int nCond = Integer.reverseBytes(data.readInt());
		int nEv = Integer.reverseBytes(data.readInt());
		
		events = new ArrayList<Event>(nEv);
		conditions = new ArrayList<Condition>(nCond);
		
		for (int i=0; i<nEv; i++)
			events.add(new Event (i+1, Integer.reverseBytes(data.readInt())));			
		
		for (int i=0; i<nCond; i++) {
			Condition c = new Condition();
			c.origPlaceN = Integer.reverseBytes(data.readInt());
			int preEventN = Integer.reverseBytes(data.readInt());
			if (preEventN != 0)
				c.presetEvent = events.get(preEventN-1);
			else
				c.presetEvent = null;
						
			int postEventN = Integer.reverseBytes(data.readInt());
			while (postEventN != 0) {
				c.postset.add(events.get(postEventN-1));
				postEventN = Integer.reverseBytes(data.readInt());
			}
			
			conditions.add(c);
		}
		
		cutoffs.clear();
		
		int nCutoff = Integer.reverseBytes(data.readInt());
		int nCorr = Integer.reverseBytes(data.readInt());
		
		while ( ! ((nCutoff==0) && (nCorr==0)) ){
			cutoffs.put(events.get(nCutoff-1), 
							(nCorr == 0)?null: events.get(nCorr-1));
			nCutoff = Integer.reverseBytes(data.readInt());
			nCorr = Integer.reverseBytes(data.readInt());
		}
		
		int nPlaces = Integer.reverseBytes(data.readInt());
		int nTrans = Integer.reverseBytes(data.readInt());
		
		tIds = new ArrayList<String>(nTrans);
		pIds = new ArrayList<String>(nPlaces);
		
		Integer.reverseBytes(data.readInt());
		
		for (int i=0; i<nPlaces; i++) {
			String id = "";
			byte c = data.readByte();
			while (c!=0) {
				id += (char)c;
				c = data.readByte();
			}
			pIds.add(id);
		}
		
		data.readByte();
		
				
		for (int i=0; i<nTrans; i++) {
			String id = "";
			byte c = data.readByte();
			while (c!=0) {
				id += (char)c;
				c = data.readByte();
			}
			tIds.add(id);
		}
		
		data.readByte();
		
		
		for (Event e : events)
			e.origTransId = tIds.get(e.origTransN-1);

		for (Condition c : conditions) 
			c.origPlaceId = pIds.get(c.origPlaceN-1);
		
	}
}