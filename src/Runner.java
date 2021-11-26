import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

//import com.hamoid.VideoExport;

import processing.core.PApplet;
import processing.core.PImage;

public class Runner extends PApplet{
	
	final String WAV_FILENAME = "untitledd final.wav";
	final int WAV_HEADER_LEN = 44;
	
	final int FPS = 30;
	final int SAMPLE_RATE = 44100;
	final int SPF = SAMPLE_RATE/FPS;
	
	byte[] samples;
	int frame = 0;	
	
	final int SPEC_SIZE = 600;
	final int SPEC_R = 255;
	final int SPEC_G = 255;
	final int SPEC_B = 255;
	final int MIN_SPEC_LEN = 10;
	final int LINES_PER_SIDE = 30;
	final float LINE_WIDTH = 6;
	final float SPACE_WIDTH = 4;
	final float OUTER_PADDING = 2;
	final int BOTTOM_FADE = 8;
	final float LINE_SCALE = 2f;
	final int CREST_MAX = 0;
	
	
//	VideoExport ve;
	PImage emblem;
	final boolean SHOW_EMBLEM = true;
	final String EMBLEM_FILENAME = "ved icon.png";
	final int EMBLEM_SIZE = 540;
	PImage backdrop;
	final String BG_FILENAME = null;
	final int SAVE_INTERVAL = 1;
	
	int MID_X;
	int MID_Y;
	
	final boolean SAVE_FRAMES = true;
	boolean fileElapsed;
	
	
	public void settings() {
		size(2304,1296);
	}
	
	
	public void setup() {
		File file = new File(WAV_FILENAME);
		InputStream fis;
		try {
			fis = new FileInputStream(file);
			byte[] buffer = new byte[(int)file.length()];
			fis.read(buffer, 0, buffer.length);
			fis.close();
			samples = new byte[(buffer.length-WAV_HEADER_LEN)/2];
			int holder = 0;
			for (int i = WAV_HEADER_LEN + 1; i < buffer.length; i+=2) {
				samples[holder] = buffer[i];
				holder++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		MID_X = width/2;
		MID_Y = height/2;
		
		if (SHOW_EMBLEM) {
			emblem = loadImage(EMBLEM_FILENAME);
			emblem.resize(0, EMBLEM_SIZE);
			emblem.loadPixels();
		}
		
		fileElapsed = false;
		
		frameRate(60);
		
//		ve = new VideoExport(this, "generated video/mVideo");
//		ve.setFrameRate(FPS*2);
//		ve.startMovie();
	}
	
	//creates png file frames
	public void draw() {
		background(0);
		
		noStroke();
		fill(SPEC_R, SPEC_G, SPEC_B);
		quad(MID_X+SPEC_SIZE/2, MID_Y,
				MID_X, MID_Y+SPEC_SIZE/2,
				MID_X-SPEC_SIZE/2, MID_Y,
				MID_X, MID_Y-SPEC_SIZE/2);
		byte[] cfs = getSamplesForFrame(frame);
		drawSpectrumLines(cfs);
		
		if (SHOW_EMBLEM) {
			drawEmblem(width/2-emblem.width/2, height/2-emblem.height/2);
		}
				
		if (!fileElapsed && SAVE_FRAMES && frameCount%SAVE_INTERVAL == 0) {
//			ve.saveFrame();
			saveFrame("generated frames/mvFrame_######.png");
		}
		if (fileElapsed) {
//			ve.endMovie();
			System.out.println("File Elapsed");
		}
		
		frame++;
//		stroke(255,0,255);
//		for (int i = 1; i < width; i+=1) {
//			line(i, samples[i+(int)(x*width)]+128,i-1, samples[i+(int)(x*width)-1]+128);
//		}
	}

	private void drawEmblem(int x, int y) {
		int w = emblem.width;
		int h = emblem.height;		
		for (int r = 0; r < h/2; r++) {
			for (int c = 0; c < w; c++) {
				if (!(c < w/2-r-1 || c > w/2+r+1)) {
					fill(emblem.pixels[r*w+c]);
					rect(x+c, y+r, 1, 1);
				}
			}
		}
		for (int r = h/2; r < h; r++) {
			for (int c = 0; c < w; c++) {
				if (!(c < w/2-h+r-1 || c > w/2+h-r+1)) {
					fill(emblem.pixels[r*w+c]);
					rect(x+c, y+r, 1, 1);
				}
			}
		}
	
	}


	private void drawSpectrumLines(byte[] s) {
		
		//finds the index of the maximum value
		byte max = s[LINES_PER_SIDE/2];
		int indexMax = LINES_PER_SIDE/2;
		for (int i = LINES_PER_SIDE+1; i < s.length - LINES_PER_SIDE; i++) {
			if (s[i] > max) {
				max = s[i];
				indexMax = i;
			} else if (s[i] == max) {
				if(abs(i-SPF/2) < abs(indexMax-SPF/2)) {
					max = s[i];
					indexMax = i;
				}
			}
		}
		
		//creates an array holding the length of each line on the spectrum
		//the value held by the middle index should be the maximum of the frame samples.
		float[] lens = new float[LINES_PER_SIDE];
		for (int i = 0; i < LINES_PER_SIDE; i++) {
			lens[i] = s[i+indexMax-LINES_PER_SIDE/2];
		}
		for (int i = 0; i < BOTTOM_FADE; i++) {
			lens[i] = (float) (lens[i]*(double)(i)/BOTTOM_FADE);
		}
		for (int i = 0; i < BOTTOM_FADE; i++) {
			lens[lens.length-1-i] = (float) (lens[lens.length-1-i]*(double)(i)/BOTTOM_FADE);
		}
		for (int i = 0; i < lens.length; i++) {
			lens[i] = Math.abs(lens[i]) + MIN_SPEC_LEN;
		}
		for (int i = 0; i < lens.length; i++) {
			lens[i] = lens[i]*LINE_SCALE;
		}
		
		
		//draws the lines on the bottom of the spectrum
		for (int i = 0; i < lens.length; i++) {
			float SPXR = MID_X + SPEC_SIZE/2 -(OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH);
			float SPXL = MID_X - SPEC_SIZE/2 + OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH;
			float SPY = MID_Y + OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH;
			ellipse(SPXL - lens[i]*cos(PI/4), SPY + lens[i]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			ellipse(SPXR + lens[i]*cos(PI/4), SPY + lens[i]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			quad(SPXL - LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2 - lens[i]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 + lens[i]*sin(PI/4),
				SPXL - LINE_WIDTH*cos(PI/4)/2 - lens[i]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 + lens[i]*sin(PI/4));
			quad(SPXR + LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
					SPXR - LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
					SPXR - LINE_WIDTH*cos(PI/4)/2 + lens[i]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 + lens[i]*sin(PI/4),
					SPXR + LINE_WIDTH*cos(PI/4)/2 + lens[i]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 + lens[i]*sin(PI/4));		
		}
		ArrayList<Integer> freqs = getFrequencies(s);
		for (int j = 0; j < freqs.size(); j++) {
			freqs.set(j,(LINES_PER_SIDE-(freqs.get(j)+5)/10));
		}
		for (int j = 0; j < freqs.size(); j++) {
			if (freqs.get(j) < 0) {
				freqs.remove(j);
				j--;
			}
		}

		float[] lens2 = new float[LINES_PER_SIDE];
		for (int j = 0; j < freqs.size(); j++) {
			lens2[freqs.get(j)] = max;
		}
		for (int j = 0; j < lens2.length; j++) {
			for (int k = 0; k < freqs.size(); k++) {
				if (abs(j-freqs.get(k)) == 1) {
					if (lens2[j] < 0.85f*max) {
						lens2[j] = 0.85f*max;
					}
				} else if (abs(j-freqs.get(k)) == 2) {
					if (lens2[j] < 0.55f*max) {
						lens2[j] = 0.55f*max;
					}
				} else if (abs(j-freqs.get(k)) == 3) {
					if (lens2[j] < 0.35f*max) {
						lens2[j] = 0.35f*max;
					}
				} else if (abs(j-freqs.get(k)) == 4) {
					if (lens2[j] < 0.2f*max) {
						lens2[j] = 0.2f*max;
					}
				} else if (abs(j-freqs.get(k)) == 4) {
					if (lens2[j] < 0.1f*max) {
						lens2[j] = 0.1f*max;
					}
				}
			}	
		}
		for (int i = 0; i < lens2.length; i++) {
			lens2[i] += (max/1.4f-CREST_MAX);
		}
		for (int i = 0; i < BOTTOM_FADE; i++) {
			lens2[i] = (float) (lens2[i]*(double)(i)/BOTTOM_FADE);
		}
		for (int i = 0; i < BOTTOM_FADE; i++) {
			lens2[lens2.length-1-i] = (float) (lens2[lens2.length-1-i]*(double)(i)/BOTTOM_FADE);
		}
		for (int i = 0; i < lens2.length; i++) {
			lens2[i] += 5;
		}
		for (int i = 0; i < lens2.length; i++) {
			lens2[i] = lens2[i]*LINE_SCALE;
		}
		
		//draws top spectrum lines
		for (int i = 0; i < lens2.length; i++) {	
			float SPXR = MID_X + SPEC_SIZE/2 -(OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH);
			float SPXL = MID_X - SPEC_SIZE/2 + OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH;
			float SPY = MID_Y - OUTER_PADDING - (i+0.5f)*LINE_WIDTH - i*SPACE_WIDTH;
			ellipse(SPXL - lens2[i]*cos(PI/4), SPY - lens2[i]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			ellipse(SPXR + lens2[i]*cos(PI/4), SPY - lens2[i]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			quad(SPXL - LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2 - lens2[i]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 - lens2[i]*sin(PI/4),
				SPXL - LINE_WIDTH*cos(PI/4)/2 - lens2[i]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 - lens2[i]*sin(PI/4));
			quad(SPXR + LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
				SPXR - LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
				SPXR - LINE_WIDTH*cos(PI/4)/2 + lens2[i]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 - lens2[i]*sin(PI/4),
				SPXR + LINE_WIDTH*cos(PI/4)/2 + lens2[i]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 - lens2[i]*sin(PI/4));				
		}
	}


	private ArrayList<Integer> getFrequencies(byte[] s) {
		ArrayList<Integer> f = new ArrayList<Integer>();
		int lc = 0;
		int diff = 0;
		while (lc < s.length && diffSign(s[lc], s[lc+1])) {
			lc += 2;
		}
		for (int i = lc+1; i < s.length-2; i+= 2) {
			if (diffSign(s[i], s[i+1])) {
				diff = i - lc;
				lc = i;
				if (diff>50) {
					f.add(diff);
				}
			}
		}
		return f;
	}


	private boolean diffSign(byte a, byte b) {
		if (Math.signum(a) == -1 && Math.signum(b) != -1) {
			return true;
		} else if(Math.signum(b) == -1 && Math.signum(a) != -1) {
			return true;
		}
		return false;
	}


	private byte[] getSamplesForFrame(int f) {
		byte[] s = new byte[SPF];
		for (int i = f*SPF; i < (f+1)*SPF; i++) {
			if (i < samples.length) {
				s[i-f*SPF] = samples[i];
			} else {
				if (i == f*SPF) {
					fileElapsed = true;
				}
				s[i-f*SPF] = 0;
			}
		}
		return s;
	}


	public static void main(String[] args) {
		PApplet.main("Runner");
	}

}