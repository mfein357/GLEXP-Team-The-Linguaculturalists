package com.linguaculturalists.phoenicia.ui;

import com.linguaculturalists.phoenicia.GameActivity;
import com.linguaculturalists.phoenicia.PhoeniciaGame;
import com.linguaculturalists.phoenicia.components.PlacedBlockSprite;
import com.linguaculturalists.phoenicia.util.PhoeniciaContext;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.ClickDetector;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.util.adt.color.Color;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.ease.EaseBackOut;

/**
 * HUD that allows the player to re-position a sprite on the map.
 */
public class SpriteMoveHUD extends PhoeniciaHUD implements ClickDetector.IClickDetectorListener {
    private PhoeniciaGame game;
    private Rectangle whiteRect;
    private PlacedBlockSprite sprite;
    private TMXTile originalLocation;
    private TMXTile newLocation;
    private SpriteMoveHandler handler;

    private ClickDetector clickDetector;

    public SpriteMoveHUD(final PhoeniciaGame game, final TMXTile startLocation, final PlacedBlockSprite sprite, final SpriteMoveHandler handler) {
        super(game.camera);
        this.game = game;
        this.sprite = sprite;
        this.originalLocation = startLocation;
        Debug.d("Start sprite Z: "+this.sprite.getZIndex());
        this.newLocation = startLocation;
        this.handler = handler;
        this.setBackgroundEnabled(false);

        final IEntityModifier fadeModifier = new LoopEntityModifier(new AlphaModifier(1,0.4f,0.6f));
        this.sprite.registerEntityModifier(fadeModifier);

        this.whiteRect = new Rectangle(GameActivity.CAMERA_WIDTH/2, 64, 200, 96, PhoeniciaContext.vboManager);
        whiteRect .setColor(Color.WHITE);
        this.attachChild(whiteRect);

        final SpriteMoveHUD hud = this;

        ITextureRegion cancelRegion = game.shellTiles.getTextureRegion(7);
        ButtonSprite cancelBlock = new ButtonSprite((whiteRect.getWidth()/2)-64, 48, cancelRegion, PhoeniciaContext.vboManager);
        cancelBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                sprite.unregisterEntityModifier(fadeModifier);
                sprite.setAlpha(1.0f);
                sprite.setZIndex(originalLocation.getTileZ());
                game.scene.sortChildren();
                if (hud.handler != null) {
                    hud.handler.onSpriteMoveCanceled(sprite);
                    hud.handler = null;
                }
                game.hudManager.pop();
            }
        });
        this.registerTouchArea(cancelBlock);
        whiteRect.attachChild(cancelBlock);

        ITextureRegion confirmRegion = game.shellTiles.getTextureRegion(6);
        ButtonSprite confirmBlock = new ButtonSprite((whiteRect.getWidth()/2)+64, 48, confirmRegion, PhoeniciaContext.vboManager);
        confirmBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                sprite.unregisterEntityModifier(fadeModifier);
                sprite.setAlpha(1.0f);
                if (hud.handler != null) {
                    hud.handler.onSpriteMoveFinished(sprite, hud.newLocation);
                    hud.handler = null;
                }
                game.hudManager.pop();
            }
        });
        this.registerTouchArea(confirmBlock);
        whiteRect.attachChild(confirmBlock);

        this.clickDetector = new ClickDetector(this);
    }

    /**
     * Animate the opacity of the sprite being moved
     */
    @Override
    public void show() {
        whiteRect.registerEntityModifier(new MoveYModifier(0.5f, -48, 64, EaseBackOut.getInstance()));
    }

    /**
     * If this HUD is being closed without the user explicitly accepting or canceling the move,
     * cancel it now
     */
    @Override
    public void close() {
        this.sprite.clearEntityModifiers();
        sprite.setAlpha(1.0f);
        if (this.handler != null) {
            this.sprite.setZIndex(this.originalLocation.getTileZ());
            this.game.scene.sortChildren();
            this.handler.onSpriteMoveCanceled(this.sprite);
        }
    }

    /**
     * Callback handler for listening to whether the user moves the sprite or now
     */
    public interface SpriteMoveHandler {
        /**
         * The player has decided not to move the sprite
         * @param sprite sprite being moved
         */
        public void onSpriteMoveCanceled(PlacedBlockSprite sprite);

        /**
         * The player confirmed the placement of the sprite
         * @param sprite sprite being moved
         * @param newLocation the new map tile location for the sprite
         */
        public void onSpriteMoveFinished(PlacedBlockSprite sprite, TMXTile newLocation);
    }

    @Override
    public void onClick(ClickDetector clickDetector, int pointerId, float sceneX, float sceneY) {
        TMXTile mapTile = game.getTileAt(sceneX, sceneY);
        if (mapTile != null) {
            this.sprite.setPosition(mapTile.getTileX() + 32, mapTile.getTileY() + 32);// Map tiles are offset by 32px
            this.sprite.setZIndex(mapTile.getTileZ()+1);
            this.game.scene.sortChildren();
            Debug.d("New sprite Z: " + this.sprite.getZIndex());
            this.newLocation = mapTile;
        }
    }

    @Override
    public boolean onSceneTouchEvent(final TouchEvent pSceneTouchEvent) {
        Debug.d("SpriteMoveHUD touched at " + pSceneTouchEvent.getX() + "x" + pSceneTouchEvent.getY());

        final boolean handled = super.onSceneTouchEvent(pSceneTouchEvent);
        if (handled) return true;

        return this.clickDetector.onManagedTouchEvent(pSceneTouchEvent);
    }
}