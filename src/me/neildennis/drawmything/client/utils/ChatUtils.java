package me.neildennis.drawmything.client.utils;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;

import me.neildennis.drawmything.client.screen.ScreenManager;

public class ChatUtils {

	public static Rectangle getStringBounds(Graphics2D g2, String str, float x, float y){
		FontRenderContext frc = g2.getFontRenderContext();
		GlyphVector gv = g2.getFont().createGlyphVector(frc, str);
		return gv.getPixelBounds(null, x, y);
	}

	public static void chat(String msg, String username){
		if (username == null)
			ScreenManager.getManager().getChat().chat(msg);
		else
			ScreenManager.getManager().getChat().chat(getPlayerColor(username) + ": " + msg);
	}

	public static String getPlayerColor(String username) {
		return username;
	}

}
