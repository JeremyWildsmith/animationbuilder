package io.github.jevaengine.builder.animationbuilder;

import io.github.jevaengine.game.IGame;
import io.github.jevaengine.game.IGameFactory;
import io.github.jevaengine.game.IRenderer;
import io.github.jevaengine.graphics.ISpriteFactory;
import io.github.jevaengine.joystick.IInputSource;
import io.github.jevaengine.ui.IWindowFactory;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;

public final class AnimationBuilderFactory implements IGameFactory
{
	@Inject
	@Named("BASE_DIRECTORY")
	private URI m_baseDirectory;
	
	private final IInputSource m_inputSource;
	private final IRenderer m_renderer;
	private final ISpriteFactory m_spriteFactory;
	private final IWindowFactory m_windowFactory;
	
	@Inject
	public AnimationBuilderFactory(IInputSource inputSource, IRenderer renderer, ISpriteFactory spriteFactory, IWindowFactory windowFactory)
	{
		m_inputSource = inputSource;
		m_renderer = renderer;
		m_spriteFactory = spriteFactory;
		m_windowFactory = windowFactory;
	}
	
	public IGame create()
	{
		//This field is injected typically. If this object is not instantiated by the ioc container
		//it will not be injected. The implementation assumes that it will be.
		assert m_baseDirectory != null: "BASE_DIRECTORY was not injected into WorldBuilder";
		
		return new AnimationBuilder(m_inputSource, m_spriteFactory, m_windowFactory, m_renderer.getResolution(), m_baseDirectory);
	}
}
