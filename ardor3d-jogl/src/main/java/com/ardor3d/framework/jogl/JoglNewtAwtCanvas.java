/**
 * Copyright (c) 2008-2012 Ardor Labs, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <http://www.ardor3d.com/LICENSE>.
 */

package com.ardor3d.framework.jogl;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import com.ardor3d.annotation.MainThread;
import com.ardor3d.framework.Canvas;
import com.ardor3d.framework.DisplaySettings;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;

public class JoglNewtAwtCanvas extends NewtCanvasAWT implements Canvas, NewtWindowContainer {

    private static final long serialVersionUID = 1L;

    private final JoglCanvasRenderer _canvasRenderer;
    private boolean _inited = false;

    private final DisplaySettings _settings;

    private final JoglDrawerRunnable _drawerGLRunnable;

    private final JoglNewtAwtInitializerRunnable _initializerRunnable;

    public JoglNewtAwtCanvas(final DisplaySettings settings, final JoglCanvasRenderer canvasRenderer) {
        super(GLWindow.create(CapsUtil.getCapsForSettings(settings)));
        _drawerGLRunnable = new JoglDrawerRunnable(canvasRenderer);
        _initializerRunnable = new JoglNewtAwtInitializerRunnable(this, settings);
        getNewtWindow().setUndecorated(true);
        _settings = settings;
        _canvasRenderer = canvasRenderer;

        setFocusable(true);
        setSize(_settings.getWidth(), _settings.getHeight());
        setIgnoreRepaint(true);
        getNewtWindow().setAutoSwapBufferMode(false);
    }

    @MainThread
    public void init() {
        if (_inited) {
            return;
        }

        // Make the window visible to realize the OpenGL surface.
        // setVisible(true);
        // Request the focus here as it cannot work when the window is not visible
        // requestFocus();
        /**
         * I do not understand why I cannot get the context earlier, I failed in getting it from addNotify() and
         * setVisible(true)
         * */
        /*
         * _canvasRenderer.setContext(getNewtWindow().getContext());
         * 
         * getNewtWindow().invoke(true, new GLRunnable() {
         * 
         * @Override public boolean run(final GLAutoDrawable glAutoDrawable) { _canvasRenderer.init(_settings, true);//
         * true - do swap in renderer. return true; } });
         */
        if (!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(_initializerRunnable);
            } catch (final InterruptedException ex) {
                ex.printStackTrace();
            } catch (final InvocationTargetException ex) {
                ex.printStackTrace();
            }
        } else {
            _initializerRunnable.run();
        }

        _inited = true;
    }

    public void draw(final CountDownLatch latch) {
        if (!_inited) {
            init();
        }

        if (isShowing()) {
            getNewtWindow().invoke(true, _drawerGLRunnable);
        }
        if (latch != null) {
            latch.countDown();
        }
    }

    public JoglCanvasRenderer getCanvasRenderer() {
        return _canvasRenderer;
    }

    @Override
    public GLWindow getNewtWindow() {
        return (GLWindow) getNEWTChild();
    }
}
