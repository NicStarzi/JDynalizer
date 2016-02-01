package edu.udo.cs.dynalysis.umlseq;

public class DiaObj {
	
	private String ref;
	private String cls;
	private String lbl;
	private int x, y, w, h;
	
	public void setText(String referenceName, String className) {
		ref = referenceName;
		cls = className;
		StringBuilder sb = new StringBuilder();
		if (ref != null) {
			sb.append(ref);
		}
		sb.append(":");
		if (cls != null) {
			sb.append(cls);
		}
		lbl = sb.toString();
	}
	
	public void setRect(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		w = width;
		h = height;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return w;
	}
	
	public int getWidth() {
		return w;
	}
	
	public int getHeight() {
		return h;
	}
	
	public int getFinalX() {
		return x + w;
	}
	
	public int getFinalY() {
		return y + h;
	}
	
	public String getText() {
		return lbl;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(ref);
		sb.append("[");
		sb.append(x);
		sb.append(", ");
		sb.append(y);
		sb.append(", ");
		sb.append(w);
		sb.append(", ");
		sb.append(h);
		sb.append("]");
		return sb.toString();
	}
	
}