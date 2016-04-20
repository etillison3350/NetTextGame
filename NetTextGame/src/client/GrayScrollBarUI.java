package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.metal.MetalScrollBarUI;

public class GrayScrollBarUI extends MetalScrollBarUI {

	@Override
	public Dimension getPreferredSize(JComponent c) {
		if (c instanceof JScrollBar) {
			if (((JScrollBar) c).getOrientation() == JScrollBar.HORIZONTAL) {
				return new Dimension(0, 8);
			} else {
				return new Dimension(8, 0);
			}
		}

		return super.getPreferredSize(c);
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
		g.setColor(Color.BLACK);
		g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
	}

	@Override
	protected JButton createDecreaseButton(int orientation) {
		JButton button = new JButton();
		button.setPreferredSize(new Dimension(0, 0));
		button.setMinimumSize(new Dimension(0, 0));
		button.setMaximumSize(new Dimension(0, 0));
		return button;
	}

	@Override
	protected JButton createIncreaseButton(int orientation) {
		return createDecreaseButton(orientation);
	}
}
