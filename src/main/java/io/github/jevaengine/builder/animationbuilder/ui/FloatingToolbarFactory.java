/* 
 * Copyright (C) 2015 Jeremy Wildsmith.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package io.github.jevaengine.builder.animationbuilder.ui;

import io.github.jevaengine.IDisposable;
import io.github.jevaengine.builder.animationbuilder.ui.SelectSpriteAnimationQueryFactory.ISelectSpriteAnimationQueryObserver;
import io.github.jevaengine.builder.animationbuilder.ui.SelectSpriteAnimationQueryFactory.SelectSpriteAnimationQuery;
import io.github.jevaengine.builder.ui.FileInputQueryFactory;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQuery;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.FileInputQueryMode;
import io.github.jevaengine.builder.ui.FileInputQueryFactory.IFileInputQueryObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory;
import io.github.jevaengine.builder.ui.MessageBoxFactory.IMessageBoxObserver;
import io.github.jevaengine.builder.ui.MessageBoxFactory.MessageBox;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.graphics.ISpriteFactory.SpriteConstructionException;
import io.github.jevaengine.graphics.Sprite;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FloatingToolbarFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/toolbar.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	private final ISpriteFactory m_spriteFactory;
	
	private final URI m_baseDirectory;
	
	public FloatingToolbarFactory(WindowManager windowManager, IWindowFactory windowFactory, ISpriteFactory spriteFactory, URI baseDirectory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
		m_spriteFactory = spriteFactory;
		m_baseDirectory = baseDirectory;
	}
	
	public FloatingToolbar create() throws WindowConstructionException
	{
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new FloatingToolbarBehaviourInjector());
		m_windowManager.addWindow(window);

		return new FloatingToolbar(window);
	}
	
	public static class FloatingToolbar implements IDisposable
	{
		private Window m_window;
		
		private FloatingToolbar(Window window)
		{
			m_window = window;
		}
		
		@Override
		public void dispose()
		{
			m_window.dispose();
		}
		
		public void setVisible(boolean isVisible)
		{
			m_window.setVisible(isVisible);
		}
		
		public void setLocation(Vector2D location)
		{
			m_window.setLocation(location);
		}
		
		public void center()
		{
			m_window.center();
		}
	}
	
	private class FloatingToolbarBehaviourInjector extends WindowBehaviourInjector
	{
		private Logger m_logger = LoggerFactory.getLogger(FloatingToolbarBehaviourInjector.class);

		private void displayMessage(String message)
		{
			try
			{
				final MessageBox msgBox = new MessageBoxFactory(m_windowManager, m_windowFactory).create(message);
				msgBox.getObservers().add(new IMessageBoxObserver() {
					@Override
					public void okay() {
						msgBox.dispose();
					}
				});
			} catch(WindowConstructionException e)
			{
				m_logger.error("Unable to construct message box", e);
			}
		}
		
		private void createSpritePreview(Sprite sprite)
		{
			try
			{
				final SelectSpriteAnimationQuery spritePreview = new SelectSpriteAnimationQueryFactory(m_windowManager, m_windowFactory).create(sprite);
			
				spritePreview.getObservers().add(new ISelectSpriteAnimationQueryObserver() {
					@Override
					public void okay(String animation)
					{
						spritePreview.dispose();
					}

					@Override
					public void cancel()
					{
						spritePreview.dispose();
					}
				});
			} catch (WindowConstructionException e)
			{
				m_logger.error("Unable to construct sprite preview window", e);
			}
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{			
			getControl(Button.class, "btnPreviewSprite").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					try
					{
						final FileInputQuery query = new FileInputQueryFactory(m_windowManager, m_windowFactory).create(FileInputQueryMode.OpenFile, "Select a sprite file to preview:", m_baseDirectory);
						query.getObservers().add(new IFileInputQueryObserver() {
							
							@Override
							public void okay(URI file)
							{
								try
								{
									createSpritePreview(m_spriteFactory.create(file));
								} catch (SpriteConstructionException e)
								{
									m_logger.error("Error occured constructing sprite.", e);
									displayMessage("Error constructing sprite. View stacktrace for more details.");
								}
								
								query.dispose();
								
							}
							
							@Override
							public void cancel() {
								query.dispose();
							}
						});
					} catch(WindowConstructionException e)
					{
						m_logger.error("Unable to construct world selection dialogue", e);
					}
				}
			});
		}
	}
}
