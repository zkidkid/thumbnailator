package net.coobird.thumbnailator.tasks.io;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.coobird.thumbnailator.builders.ThumbnailParameterBuilder;
import net.coobird.thumbnailator.geometry.AbsoluteSize;
import net.coobird.thumbnailator.geometry.Coordinate;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.geometry.Region;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import net.coobird.thumbnailator.test.BufferedImageComparer;

import org.junit.Test;


public class InputStreamImageSourceTest
{
	@Test(expected=NullPointerException.class)
	public void givenNullInputStream() throws IOException
	{
		try
		{
			// given
			// when
			new InputStreamImageSource(null);
		}
		catch (NullPointerException e)
		{
			// then
			assertEquals("InputStream cannot be null.", e.getMessage());
			throw e;
		}
	}
	
	@Test
	public void fileExists_Png() throws IOException
	{
		// given
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream("test-resources/Thumbnailator/grid.png"));
		
		// when
		BufferedImage img = source.read();
		
		// then
		assertEquals(100, img.getWidth());
		assertEquals(100, img.getHeight());
		assertEquals("png", source.getInputFormatName());
	}
	
	@Test
	public void fileExists_Jpeg() throws IOException
	{
		// given
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream("test-resources/Thumbnailator/grid.jpg"));
		
		// when
		BufferedImage img = source.read();
		
		// then
		assertEquals(100, img.getWidth());
		assertEquals(100, img.getHeight());
		assertEquals("JPEG", source.getInputFormatName());
	}
	
	@Test
	public void fileExists_Bmp() throws IOException
	{
		// given
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream("test-resources/Thumbnailator/grid.bmp"));
		
		// when
		BufferedImage img = source.read();
		
		// then
		assertEquals(100, img.getWidth());
		assertEquals(100, img.getHeight());
		assertEquals("bmp", source.getInputFormatName());
	}
	
	@Test(expected=UnsupportedFormatException.class)
	public void cannotDetermineImageFormat() throws IOException
	{
		// given
		InputStream is = mock(InputStream.class);
		when(is.read()).thenThrow(new IOException("Failed on read."));
		when(is.read(any(byte[].class))).thenThrow(new IOException("Failed on read."));
		when(is.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("Failed on read."));
		
		InputStreamImageSource source = new InputStreamImageSource(is);
		
		// when
		try
		{
			source.read();
			fail();
		}
		catch (UnsupportedFormatException e)
		{
			// then
			assertEquals("No suitable ImageReader found for source data.", e.getMessage());
			throw e;
		}
	}
	
	@Test(expected=IOException.class)
	public void badImage_Png() throws IOException
	{
		try
		{
			// given
			byte[] bytes = new byte[100]; 
			new FileInputStream("test-resources/Thumbnailator/grid.png").read(bytes);
			
			ByteArrayInputStream is = new ByteArrayInputStream(bytes);
			InputStreamImageSource source = new InputStreamImageSource(is);
			
			// when
			source.read();
		}
		catch (IOException e)
		{
			// then
			assertEquals("Error reading PNG image data", e.getMessage());
			throw e;
		}
		fail();
	}
	
	@Test(expected=IllegalStateException.class)
	public void fileExists_getInputFormatNameBeforeRead() throws IOException
	{
		// given
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream("test-resources/Thumbnailator/grid.png"));
		
		try
		{
			// when
			source.getInputFormatName();
		}
		catch (IllegalStateException e)
		{
			// then
			assertEquals("Input has not been read yet.", e.getMessage());
			throw e;
		}
	}
	
	/*
	 *
	 *     +------+-----------+
	 *     |XXXXXX|           |
	 *     |XXXXXX|           |
	 *     +------+           |
	 *     |      region      |
	 *     |                  |
	 *     |                  |
	 *     |                  |
	 *     |                  |
	 *     +------------------+
	 *                        source
	 */
	@Test
	public void appliesSourceRegion() throws IOException
	{
		// given
		File sourceFile = new File("test-resources/Thumbnailator/grid.png");
		BufferedImage sourceImage = ImageIO.read(sourceFile);
		
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream(sourceFile));
		source.setThumbnailParameter(
				new ThumbnailParameterBuilder()
					.region(new Region(Positions.TOP_LEFT, new AbsoluteSize(40, 40)))
					.size(20, 20)
					.build()
		);
		
		// when
		BufferedImage img = source.read();
			
		// then
		BufferedImage expectedImg = sourceImage.getSubimage(0, 0, 40, 40);
		assertTrue(BufferedImageComparer.isRGBSimilar(expectedImg, img));
	}
	
	/*
	 *
	 *     +------------------+ source
	 *     |  +------------------+       
	 *     |  |XXXXXXXXXXXXXXX|  |
	 *     |  |XXXXXXXXXXXXXXX|  |
	 *     |  |XX  final  XXXX|  |
	 *     |  |XX  region XXXX|  |
	 *     |  |XXXXXXXXXXXXXXX|  |
	 *     |  |XXXXXXXXXXXXXXX|  |
	 *     |  |XXXXXXXXXXXXXXX|  |
	 *     +--|---------------+  |
	 *        +------------------+
	 *                             region
	 */
	@Test
	public void appliesSourceRegionTooBig() throws IOException
	{
		// given
		File sourceFile = new File("test-resources/Thumbnailator/grid.png");
		BufferedImage sourceImage = ImageIO.read(sourceFile);
		
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream(sourceFile));
		source.setThumbnailParameter(
				new ThumbnailParameterBuilder()
					.region(new Region(new Coordinate(20, 20), new AbsoluteSize(100, 100)))
					.size(80, 80)
					.build()
		);
		
		// when
		BufferedImage img = source.read();
		
		// then
		BufferedImage expectedImg = sourceImage.getSubimage(20, 20, 80, 80);
		assertTrue(BufferedImageComparer.isRGBSimilar(expectedImg, img));
	}
	
	/*
	 *   +-----------------+
	 *   |                 |
	 *   | +---------------|--+
	 *   | |XXXXXXXXXXXXXXX|  |
	 *   | |XXXXXXXXXXXXXXX|  |
	 *   | |XXXX final XXXX|  |
	 *   | |XXXX regionXXXX|  |
	 *   | |XXXXXXXXXXXXXXX|  |
	 *   | |XXXXXXXXXXXXXXX|  |
	 *   +-----------------+  |
	 *     |                region
	 *     +------------------+
	 *                        source
	 */
	@Test
	public void appliesSourceRegionBeyondOrigin() throws IOException
	{
		// given
		File sourceFile = new File("test-resources/Thumbnailator/grid.png");
		BufferedImage sourceImage = ImageIO.read(sourceFile);
		
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream(sourceFile));
		source.setThumbnailParameter(
				new ThumbnailParameterBuilder()
					.region(new Region(new Coordinate(-20, -20), new AbsoluteSize(100, 100)))
					.size(80, 80)
					.build()
		);
		
		// when
		BufferedImage img = source.read();
		
		// then
		BufferedImage expectedImg = sourceImage.getSubimage(0, 0, 80, 80);
		assertTrue(BufferedImageComparer.isRGBSimilar(expectedImg, img));
	}
	
	@Test
	public void appliesSourceRegionNotSpecified() throws IOException
	{
		// given
		File sourceFile = new File("test-resources/Thumbnailator/grid.png");
		BufferedImage sourceImage = ImageIO.read(sourceFile);
		
		InputStreamImageSource source = new InputStreamImageSource(new FileInputStream(sourceFile));
		source.setThumbnailParameter(
				new ThumbnailParameterBuilder()
					.size(20, 20)
					.build()
		);
		
		// when
		BufferedImage img = source.read();
		
		// then
		assertTrue(BufferedImageComparer.isRGBSimilar(sourceImage, img));
	}
}