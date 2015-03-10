package io.github.jevaengine.builder.animationbuilder.ui;



import io.github.jevaengine.IDisposable;
import io.github.jevaengine.graphics.AnimationState;
import io.github.jevaengine.graphics.IImmutableAnimation;
import io.github.jevaengine.graphics.IRenderable;
import io.github.jevaengine.graphics.Sprite;
import io.github.jevaengine.graphics.Sprite.NoSuchSpriteAnimation;
import io.github.jevaengine.math.Vector2D;
import io.github.jevaengine.ui.Button;
import io.github.jevaengine.ui.Button.IButtonPressObserver;
import io.github.jevaengine.ui.IWindowFactory;
import io.github.jevaengine.ui.IWindowFactory.WindowConstructionException;
import io.github.jevaengine.ui.Label;
import io.github.jevaengine.ui.NoSuchControlException;
import io.github.jevaengine.ui.Timer;
import io.github.jevaengine.ui.Timer.ITimerObserver;
import io.github.jevaengine.ui.Viewport;
import io.github.jevaengine.ui.Window;
import io.github.jevaengine.ui.WindowBehaviourInjector;
import io.github.jevaengine.ui.WindowManager;
import io.github.jevaengine.util.IObserverRegistry;
import io.github.jevaengine.util.Observers;

import java.awt.Graphics2D;
import java.net.URI;

public final class SelectSpriteAnimationQueryFactory
{
	private static final URI WINDOW_LAYOUT = URI.create("local:///ui/windows/selectSpriteAnimation.jwl");
	
	private final WindowManager m_windowManager;
	private final IWindowFactory m_windowFactory;
	
	public SelectSpriteAnimationQueryFactory(WindowManager windowManager, IWindowFactory windowFactory)
	{
		m_windowManager = windowManager;
		m_windowFactory = windowFactory;
	}
	
	public SelectSpriteAnimationQuery create(Sprite sprite) throws WindowConstructionException
	{
		Observers observers = new Observers();
			
		Window window = m_windowFactory.create(WINDOW_LAYOUT, new SelectSpriteAnimationQueryBehaviourInjector(observers, sprite));
		m_windowManager.addWindow(window);
			
		window.center();
		return new SelectSpriteAnimationQuery(observers, window);
	}
	
	public static class SelectSpriteAnimationQuery implements IDisposable
	{
		private final Observers m_observers;
		
		private final Window m_window;
		
		private SelectSpriteAnimationQuery(Observers observers, Window window)
		{
			m_observers = observers;
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
		
		public IObserverRegistry getObservers()
		{
			return m_observers;
		}
	}
	
	private class SelectSpriteAnimationQueryBehaviourInjector extends WindowBehaviourInjector
	{
		private final Observers m_observers;
		private final Sprite m_sprite;

		private final String[] m_animations;
		private int m_currentAnimation = 0;
		
		public SelectSpriteAnimationQueryBehaviourInjector(Observers observers, Sprite sprite)
		{
			m_observers = observers;
			m_sprite = sprite;
			m_animations = m_sprite.getAnimations();
		}
		
		@Override
		protected void doInject() throws NoSuchControlException
		{
			final Viewport spriteViewport = getControl(Viewport.class, "spriteViewport");
			final Label lblAnimationName = getControl(Label.class, "lblAnimation");
			
			final Timer timer = new Timer();
			timer.getObservers().add(new ITimerObserver() {
				@Override
				public void update(int deltaTime)
				{
					IImmutableAnimation currentAnimation = m_sprite.getAnimation(m_sprite.getCurrentAnimation());
					lblAnimationName.setText(currentAnimation.getName() + ":" + (currentAnimation.getCurrentFrameIndex() + 1) + "/" + currentAnimation.getTotalFrames());
					m_sprite.update((int)(deltaTime * 0.1F));
				}
			});
			addControl(timer);
			
			spriteViewport.setView(new IRenderable() {
				@Override
				public void render(Graphics2D g, int x, int y, float scale)
				{
					m_sprite.render(g, x + spriteViewport.getBounds().width / 2, y + spriteViewport.getBounds().height / 2, scale);
				}
			});
			
			getControl(Button.class, "btnNext").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					if(m_animations.length > 0)
					{
						m_currentAnimation = (m_currentAnimation + 1) % m_animations.length;
						
						try
						{
							m_sprite.setAnimation(m_animations[m_currentAnimation], AnimationState.PlayToEnd);
						} catch(NoSuchSpriteAnimation e)
						{
							//Since sprites are discovered and then accessed programatically, the only way for this exception to occur is via
							//programming error.
							throw new RuntimeException(e);
						}
					}
				}
			});
			
			getControl(Button.class, "btnLast").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					if(m_animations.length > 0)
					{
						if(m_currentAnimation == 0)
							m_currentAnimation = m_animations.length - 1;
						else
							m_currentAnimation--;

						try {
							m_sprite.setAnimation(m_animations[m_currentAnimation], AnimationState.PlayToEnd);
						} catch (NoSuchSpriteAnimation e)
						{
							//Since these animations are chosen by our logic, and their ability to
							//occur is in our control, this should be casted to a runtime exception as it
							//is a programmer error ad is avoidable with proper implementation.
							throw new RuntimeException(e);
						}
					}
				}
			});
			
			getControl(Button.class, "btnOkay").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(ISelectSpriteAnimationQueryObserver.class).okay(m_animations.length < 0 ? "" : m_animations[m_currentAnimation]);
				}
			});
			
			getControl(Button.class, "btnCancel").getObservers().add(new IButtonPressObserver() {
				@Override
				public void onPress() {
					m_observers.raise(ISelectSpriteAnimationQueryObserver.class).cancel();
				}
			});
		}
	}
	
	public interface ISelectSpriteAnimationQueryObserver
	{
		void okay(String animation);
		void cancel();
	}
}
