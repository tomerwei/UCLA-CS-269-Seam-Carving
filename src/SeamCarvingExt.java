import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;


public class SeamCarvingExt {

	private String fileName;
	private BufferedImage inImg;
	private BufferedImage outImg;
	private int height;
	private ColorModel colorModel;	
	private final int[][] sobelMatrixX = {{-1,0,1}, {-2,0,2},{-1,0,1}};
	private final int[][] sobelMatrixY = {{-1,-2,-1}, {0,0,0},{1,2,1}};
	private double [][] edgeMap;	
	private int[][] outputMap;
	private double [][] seamCarveMap;

	public SeamCarvingExt( String fileName, int newWidth ) {
		if(init(fileName, newWidth)) return;		
		createEdgeMap();
		int widthPos = inImg.getWidth();
		for(;widthPos > newWidth; --widthPos) {
			LinkedList<Location> seam = doSeamCarving(widthPos);
			removeSeam(seam,widthPos, newWidth);
		}
		saveImage(newWidth);
		System.out.println("Done.");
	}

	private boolean init(String fileName, int newWidth) {
		File in = null;
		try {			
			in = new File(fileName);		
			this.inImg = ImageIO.read(in);		
		} 
		catch (IOException e) {			
			e.printStackTrace();
			System.out.println("Error: Cannot read File, exiting");
			return true;
		}
		if(inImg.getWidth() <= newWidth) {
			System.out.println("Error: New width must be smaller than original. Exiting.");
			return true;
		}
		else if(newWidth<=1){
			System.out.println("Error: New width must be larger than 1. Exiting.");
			return true;
		}
		
		this.fileName = in.getName();
		this.colorModel = java.awt.image.DirectColorModel.getRGBdefault();		
		this.height = inImg.getHeight();
		this.outImg = new BufferedImage(newWidth, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
		this.edgeMap = new double[inImg.getWidth()][height];
		this.seamCarveMap = new double[inImg.getWidth()][height];
		this.outputMap = new int[inImg.getWidth()][height];
		for(int y=0 ; y< height;++y) {
			for(int x=0; x<inImg.getWidth() ; ++x) {
				int rgb = inImg.getRGB(x, y);				
				outputMap[x][y] = rgb;
			}
		}
		System.out.println("Pecentage Complete: ");
		return false;
	}	
		
	private double getEdgeValueX(int x, int y) {
		double resultX =0, resultY =0, result=0;
		int realX = 0;
		int realY = 0;
		for(int sobelPosX = 0, i = (x-1); i<= (x+1); ++i, ++sobelPosX) {
			for(int sobelPosY=0, j= (y-1); j<=(y+1); ++j, ++sobelPosY) {
				if(i<0) realX = 0;
				else if(i>=inImg.getWidth()) realX = inImg.getWidth()-1;
				else realX = i;				
				if(j<0) realY = 0;
				else if(j>=height) realY = height-1;
				else realY = j;												
				int red = colorModel.getRed(inImg.getRGB(realX, realY));
				int green = colorModel.getBlue(inImg.getRGB(realX, realY));
				int blue = colorModel.getGreen(inImg.getRGB(realX, realY));
				double total =(red *0.3) + (green*0.59) + (blue*0.11);				
				double tmpGx = sobelMatrixX[sobelPosX][sobelPosY]*total;
				double tmpGy = sobelMatrixY[sobelPosX][sobelPosY]*total;
				resultX += tmpGx; resultY += tmpGy;
			}			
		}
		result = Math.sqrt((Math.pow(resultX, 2) + Math.pow(resultY, 2)));
		return result;
	}
	
	private void createEdgeMap() {		
		double maxValue=0;
		for(int x=0; x< inImg.getWidth(); ++x) {
			for(int y=0; y<height; ++y) {
				this.edgeMap[x][y] = getEdgeValueX(x,y);		
				if(edgeMap[x][y]>maxValue) maxValue=edgeMap[x][y];
			}
		}									
	}
	
	private double getMinSeamCellValue(int x, int y, int width) {
		double min = seamCarveMap[x][y-1];		
		if(x>0) {
			if(min>seamCarveMap[x-1][y-1]) min=seamCarveMap[x-1][y-1];
			if(x<(width-1)) {
				if(min>seamCarveMap[x+1][y-1]) min=seamCarveMap[x+1][y-1];
			}
		}
		else if(x<(width-1)) {
			if(min>seamCarveMap[x+1][y-1]) min=seamCarveMap[x+1][y-1];
		}
		return min;
	}
	
	
	private LinkedList<Location> seamPath(int height, int width) {
		LinkedList<Location> result = new LinkedList<Location>();
		int ypos = height-1;
		int xpos = 0;
		double minValue = seamCarveMap[0][ypos];
		for(int x=0; x<width; ++x) {
			if(seamCarveMap[x][ypos]<minValue) xpos = x;
		}		
		result.add(new Location(xpos,ypos));				
		for(ypos-=1 ;ypos>=0 ; --ypos) {						
			minValue = seamCarveMap[xpos][ypos];
			if((xpos > 0) && (xpos<(width-1))) {
				int delta =0;
				if(seamCarveMap[xpos-1][ypos]<minValue) {
					minValue = seamCarveMap[xpos-1][ypos];
					delta = -1;
				}
				if(seamCarveMap[xpos+1][ypos]<minValue) {
					delta = 1;
				}
				xpos+=delta;				
			}
			else if(xpos > 0) {
				if(seamCarveMap[xpos-1][ypos]<minValue) xpos-=1;
			}
			else if(xpos < (width-1)){
				if(seamCarveMap[xpos+1][ypos]<minValue) xpos+=1;
			}			
			result.add(new Location(xpos,ypos));			
		}				
		return result;	
	}
		
	private LinkedList<Location> doSeamCarving(int width) {
		for(int x=0; x<width; ++x) {
			this.seamCarveMap[x][0] = edgeMap[x][0];		
		}		
		for(int y=1; y<height; ++y) {
			for(int x=0; x< width; ++x) {				
				this.seamCarveMap[x][y] = getMinSeamCellValue(x,y,width) + edgeMap[x][y];
			}			
		}
		LinkedList<Location> seam = seamPath(height,width);
		return seam;
	}

	private void removeSeam(LinkedList<Location> seam, int oldWidth, int newWidth) {
		for(Location loc: seam) {			
			this.edgeMap[loc.getX()][loc.getY()] = -1;
		}		
		for(int y=0; y<height; ++y) {
			for(int x=0; x<oldWidth; ++x) {		
				if(-1 == edgeMap[x][y]) {					
					if(x < (oldWidth-1)) {
						for(int xtmp=x; xtmp<(oldWidth-1); ++xtmp) {
							this.edgeMap[xtmp][y] = edgeMap[xtmp+1][y];
							this.outputMap[xtmp][y] = outputMap[xtmp+1][y];
						}
						break;
					}					
				}
				else {
					this.edgeMap[x][y] = edgeMap[x][y];
					this.outputMap[x][y] = outputMap[x][y];
				}				
			}
		}
		//System.out.print("\b\b\b\b\b\b");		
		double percent =    (double)(inImg.getWidth() - oldWidth )/ (double)(inImg.getWidth() - newWidth);
		System.out.printf("%.3f\n", percent);		
	}
	
	private void saveImage(int newWidth) {		
		for(int x=0; x< newWidth; ++x) {
			for(int y=0; y<height; ++y) {				
				int result = outputMap[x][y];
				outImg.setRGB(x,y, result);																				
			}
		}
		int prefixPos = fileName.lastIndexOf('.');
		String prefix = fileName.substring(0, prefixPos);
		String suffix = fileName.substring(prefixPos);
		try {
			System.out.println(prefix + "_new_width_" + newWidth + suffix);
			File out = new File(prefix + "_new_width_" + newWidth + suffix);		
			ImageIO.write(outImg, fileName.substring(prefixPos+1) , out);	
		} 
		catch (IOException e) {
			System.out.println("Error: Cannot output file. Exiting");			
		}				
	}
	
	private class Location {
		int x;
		int y;
		
		private Location(int x, int y) {
			this.x= x;
			this.y =y;
		}
		
		private int getX() {
			return x;
		}

		private int getY() {
			return y;
		}
		
		@Override
		public String toString() {
			return "(" + getX() + ", " + getY() + ")";
		}
		
	}
	
	public static void main (String [] args) {
		if(args.length != 2) {
			System.out.println("Error: Incorrect number of arguments. Please enter file path and new width" );
			System.exit(1);
		}
		try{
			Integer newWidth = null;
			try{
				newWidth = Integer.parseInt(args[1]);
			}
			catch(NumberFormatException e) {
				System.out.println("Error: 2nd argument is not an Integer. Exiting." );
				System.exit(1);
			}
			if(null == newWidth) {
				System.out.println("Error: 2nd argument is not an Integer. Exiting." );
				System.exit(1);
			}
			else {
				new SeamCarving(args[0],newWidth);
			}
		}
		catch(Exception e) {
			System.out.println("General Error: program failed. Exiting");
			System.exit(1);	
		}

	}
	
}
