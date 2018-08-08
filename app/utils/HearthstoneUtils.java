package utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import enums.Hearthstone.Hero;
import models.hibernateModels.BaseHearthstoneCard;

public class HearthstoneUtils {
	
	public static void writeVarInt(DataOutputStream dos, int value) throws IOException {
		//taken from http://wiki.vg/Data_types, altered slightly
		do {
			byte temp = (byte) (value & 0b01111111);
			
			// Note: >>> means that the sign bit is shifted with the rest of the
			// number rather than being left alone
			value >>>= 7;
				if (value != 0) {
					temp |= 0b10000000;
				}
				dos.writeByte(temp);
		} while (value != 0);
	}
	
	public static String getCodeFromList(Hero heroClass, boolean isStandard,List<BaseHearthstoneCard> cards) {
		ByteArrayOutputStream baos = null;
		DataOutputStream dos = null;
		if (heroClass != Hero.NEUTRAL) {
			try {
				baos = new ByteArrayOutputStream();
				dos = new DataOutputStream(baos);

				HearthstoneUtils.writeVarInt(dos, 0); // always zero
				HearthstoneUtils.writeVarInt(dos, 1); // encoding version number
				HearthstoneUtils.writeVarInt(dos, isStandard ? 2 : 1); // standard = 2, wild = 1
				HearthstoneUtils.writeVarInt(dos, 1); // number of heroes in heroes array, always 1
				HearthstoneUtils.writeVarInt(dos, heroClass.dbId); // DBF ID of hero
				List<List<Integer>> deckOrdered = orderedDeck(cards);

				int length = deckOrdered.get(0).size();
				HearthstoneUtils.writeVarInt(dos, length); // number of 1-quantity cards

				for (int i = 0; i < length; i++) {
					HearthstoneUtils.writeVarInt(dos, deckOrdered.get(0).get(i));
				}

				length = deckOrdered.get(1).size();
				HearthstoneUtils.writeVarInt(dos, length); // number of 1-quantity cards

				for (int i = 0; i < length; i++) {
					HearthstoneUtils.writeVarInt(dos, deckOrdered.get(1).get(i));
				}

				for (int i = 2; i < deckOrdered.size()-2; i++) {

				}
				HearthstoneUtils.writeVarInt(dos, 0); //the number of cards that have quantity greater than 2. Always 0 for constructed

				dos.flush(); //flushes the output stream to the byte array output stream

				if (baos != null)
					baos.close();
				if (dos != null)
					dos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			String deckString = Base64.getEncoder().encodeToString(baos.toByteArray()); //encode the byte array to a base64 string
			return deckString;
		}
		return null;
	}
	
	private static List<List<Integer>> orderedDeck(List<BaseHearthstoneCard> cards) {
		List<List<Integer>> res = new ArrayList<>();
		res.add(new ArrayList<>());
		res.add(new ArrayList<>());
		for (BaseHearthstoneCard c : cards) {
			int id = c.dbId.intValue();
			for (int i = 0; i < res.size(); i++) {
				if (res.get(i).contains(id)) {
					res.get(i).remove((Object) id);
					if (i + 1 == res.size()) {
						res.add(new ArrayList<>());
					}
					res.get(i+1).add(id);
					break;
				} else if (i +1 == res.size()) {
					res.get(0).add(id);
				}
			}

		}
		return res;
	}
}
