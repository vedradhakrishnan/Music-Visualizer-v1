import java.io.*;
import java.util.*;

import processing.core.*;

import java.awt.*;

public class Visualize extends PApplet {

	final int WAV_HEADER_LEN = 44;
	
	//SETTINGS
	final String WAV_FILENAME = "sine_square.wav";
	final int FPS = 30;
	final int SAMPLE_RATE = 44100;
	final int SPF = SAMPLE_RATE/FPS;
	final int SPAN = SPF * 4;
	
	//INCLUDED FREQUENCIES
	final int FD_LEN = 216;
	final double FEXP_START = 4.;
	final double FEXP_STEP = 0.035;
	
	int[] left;
	int[] right;

	int frame = 0;	
	
	final int SPEC_SIZE = 600;
	final int SPEC_R = 255;
	final int SPEC_G = 255;
	final int SPEC_B = 255;
	final int LINES_PER_SIDE = 30;
	final float LINE_WIDTH = 6;
	final float SPACE_WIDTH = 4;
	final float OUTER_PADDING = 2;
	final int BOTTOM_FADE = 8;
	
	//VISUAL TUNING
	final int CREST_MAX = 0;
	final int MIN_SPEC_LEN = 6;
	final float GAIN = 7.5f;
	final float EXP = 2f;
	
	
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


	public static void main(String[] args) {
		PApplet.main("Visualize");
	}
	
	
	public void settings() {
		size(1920, 1080);
	}
	
	public void setup() {
		import_samples(WAV_FILENAME, WAV_HEADER_LEN);
		
		MID_X = width/2;
		MID_Y = height/2;
		
		if (SHOW_EMBLEM) {
			emblem = loadImage(EMBLEM_FILENAME);
			emblem.resize(0, EMBLEM_SIZE);
			emblem.loadPixels();
		}
		
		fileElapsed = false;
		
		frameRate(60);
		noStroke();
	}
	
	public void draw() {
		int t1 = frame * SPF;
		int t2 = t1 + SPAN;

		//discrete fourier transform to generate frequency domains
		float[] fd_left = dft(left, t1, t2);
		float[] fd_right = dft(right, t1, t2);
		
		//draw objects items
		background(0);
		
		fill(SPEC_R, SPEC_G, SPEC_B);
		quad(MID_X+SPEC_SIZE/2, MID_Y,
				MID_X, MID_Y+SPEC_SIZE/2,
				MID_X-SPEC_SIZE/2, MID_Y,
				MID_X, MID_Y-SPEC_SIZE/2);
		
		draw_spectrum_lines(fd_left, fd_right);
		
		
		
		if (SHOW_EMBLEM)
			draw_emblem(width/2-emblem.width/2, height/2-emblem.height/2);
				
		if (SAVE_FRAMES && frameCount%SAVE_INTERVAL == 0) //save frames
			saveFrame("generated frames/mvFrame_######.png");

		if (++frame * SPF + SPAN >= left.length) {
			fileElapsed = true;
			System.out.println("File Elapsed");
			exit();
		}

		

	}
	

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////	

	
	private float[] dft(int[] a, int t1, int t2) { //dft algorithm
		float[] freq_domain = new float[FD_LEN];
		int index = 0;
		
		for (double f_exp = FEXP_START; f_exp < FEXP_START + FEXP_STEP*(FD_LEN - 1); f_exp+=FEXP_STEP) {
			double omega = Math.pow(2, f_exp);
			
			double sum_x = 0;
			double sum_y = 0;
			for (int t = t1; t < t2; t++) {
				double theta = 2 * Math.PI * omega * t / SAMPLE_RATE;
				
				sum_x += a[t] * Math.cos(theta) / SPAN;
				sum_y += a[t] * Math.sin(theta) / SPAN;
			}
			
			freq_domain[index] = (float) Math.sqrt(sum_x * sum_x + sum_y * sum_y);
			index++;
		}
		
		return freq_domain;	
	}

	private void draw_spectrum_lines(float[] left, float[] right) {
		float[] left_lens = convolute(left);
		float[] right_lens = convolute(right);
		
		//draws the lines on the bottom of the spectrum
		for (int i = 0; i < LINES_PER_SIDE; i++) {
			int j = LINES_PER_SIDE - (i + 1);
			
			float SPXR = MID_X + SPEC_SIZE/2 -(OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH);
			float SPXL = MID_X - SPEC_SIZE/2 + OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH;
			float SPY = MID_Y + OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH;
			
			//bottom left
			ellipse(SPXL - left_lens[j]*cos(PI/4), SPY + left_lens[j]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			quad(SPXL - LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2 - left_lens[j]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 + left_lens[j]*sin(PI/4),
				SPXL - LINE_WIDTH*cos(PI/4)/2 - left_lens[j]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 + left_lens[j]*sin(PI/4));
			
			//bottom right
			ellipse(SPXR + right_lens[j]*cos(PI/4), SPY + right_lens[j]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			quad(SPXR + LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
					SPXR - LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
					SPXR - LINE_WIDTH*cos(PI/4)/2 + right_lens[j]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 + right_lens[j]*sin(PI/4),
					SPXR + LINE_WIDTH*cos(PI/4)/2 + right_lens[j]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 + right_lens[j]*sin(PI/4));		
		}
		
		//draws the lines on the top of the spectrum!
		for (int i = 0; i < LINES_PER_SIDE; i++) {
			int j = i + LINES_PER_SIDE;
			
			float SPXR = MID_X + SPEC_SIZE/2 -(OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH);
			float SPXL = MID_X - SPEC_SIZE/2 + OUTER_PADDING + (i+0.5f)*LINE_WIDTH + i*SPACE_WIDTH;
			float SPY = MID_Y - OUTER_PADDING - (i+0.5f)*LINE_WIDTH - i*SPACE_WIDTH;
			
			ellipse(SPXL - left_lens[j]*cos(PI/4), SPY - left_lens[j]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			quad(SPXL - LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
				SPXL + LINE_WIDTH*cos(PI/4)/2 - left_lens[j]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 - left_lens[j]*sin(PI/4),
				SPXL - LINE_WIDTH*cos(PI/4)/2 - left_lens[j]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 - left_lens[j]*sin(PI/4));
			
			ellipse(SPXR + right_lens[j]*cos(PI/4), SPY - right_lens[j]*sin(PI/4), LINE_WIDTH, LINE_WIDTH);
			quad(SPXR + LINE_WIDTH*cos(PI/4)/2, SPY + LINE_WIDTH*sin(PI/4)/2,
				SPXR - LINE_WIDTH*cos(PI/4)/2, SPY - LINE_WIDTH*sin(PI/4)/2,
				SPXR - LINE_WIDTH*cos(PI/4)/2 + right_lens[j]*cos(PI/4), SPY - LINE_WIDTH*sin(PI/4)/2 - right_lens[j]*sin(PI/4),
				SPXR + LINE_WIDTH*cos(PI/4)/2 + right_lens[j]*cos(PI/4), SPY + LINE_WIDTH*sin(PI/4)/2 - right_lens[j]*sin(PI/4));				
		}
	}

	private float[] convolute(float[] a) {
		float[] out = new float [LINES_PER_SIDE * 2];
		for (int i = 0; i < a.length; i++) {
			int x = (int) ((i + 0.5) * (double)(out.length) / (FD_LEN));
//			System.out.println(x - i);
			//blending
			if (x-3 >= 0) out[x-3] += 0.125f * a[i];
			if (x-2 >= 0) out[x-2] += 0.25f * a[i];
			if (x-1 >= 0) out[x-1] += 0.5f * a[i];
			out[x] += 0.25f * a[i];
			if (x+1 < out.length) out[x+1] += 0.5f * a[i];
			if (x+2 < out.length) out[x+2] += 0.25f * a[i];
			if (x+3 < out.length) out[x+3] += 0.125f * a[i];
		}
		
		//fading
		for (int i = 0; i < BOTTOM_FADE; i++) {
			out[i] = (float) (out[i] * (double)(i)/BOTTOM_FADE);
			out[LINES_PER_SIDE - i - 1] = (float) (out[LINES_PER_SIDE - i - 1] * (double)(i)/BOTTOM_FADE);
			out[LINES_PER_SIDE + i] = (float) (out[LINES_PER_SIDE + i] * (double)(i)/BOTTOM_FADE);
			out[LINES_PER_SIDE * 2 - i - 1] = (float) (out[LINES_PER_SIDE * 2 - i - 1] * (double)(i)/BOTTOM_FADE);
		}
		
		//gain
		for (int i = 0; i < out.length; i++) {
			out[i] *= Math.pow(EXP, (double)(i - LINES_PER_SIDE) / LINES_PER_SIDE);
			out[i] = GAIN * out[i] + MIN_SPEC_LEN;
		}
		
		return out;
	}


	private void draw_emblem(int x, int y) {
		int w = emblem.width;
		int h = emblem.height;	
		
		for (int r = 0; r < h/2; r++)
			for (int c = 0; c < w; c++)
				if (!(c < w/2-r-1 || c > w/2+r+1)) {
					fill(emblem.pixels[r*w+c]);
					rect(x+c, y+r, 1, 1);
				}

		for (int r = h/2; r < h; r++)
			for (int c = 0; c < w; c++)
				if (!(c < w/2-h+r-1 || c > w/2+h-r+1)) {
					fill(emblem.pixels[r*w+c]);
					rect(x+c, y+r, 1, 1);
				}
	}

	private void import_samples(String filename, int header) {
		InputStream fis;
		
		
		int[] samples;
		try {
			File file = new File(filename);
			fis = new FileInputStream(file);
			
			byte[] buffer = new byte[(int)file.length()];
			fis.read(buffer, 0, buffer.length);
			fis.close();
			samples = new int[(buffer.length-header)/2];
			int holder = 0;
			for (int i = header + 1; i < buffer.length; i+=2) {
				samples[holder] = buffer[i];
				holder++;
			}
			
			int len = SPAN * (1 + samples.length / 2 / SPAN);
			left = new int[len];
			right = new int[len];

			for (int i = 0; i < samples.length / 2; i++) {
				left[i] = samples[2*i];
				right[i] = samples[2*i + 1];
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
