package com.linguaculturalists.phoenicia.models;

import android.content.Context;

import com.linguaculturalists.phoenicia.PhoeniciaGame;
import com.linguaculturalists.phoenicia.components.MapBlockSprite;
import com.linguaculturalists.phoenicia.components.PlacedBlockSprite;
import com.linguaculturalists.phoenicia.locale.Game;
import com.linguaculturalists.phoenicia.locale.Word;
import com.linguaculturalists.phoenicia.ui.SpriteMoveHUD;
import com.linguaculturalists.phoenicia.util.GameFonts;
import com.linguaculturalists.phoenicia.util.GameTextures;
import com.linguaculturalists.phoenicia.util.PhoeniciaContext;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.migration.Migrator;

import org.andengine.entity.IEntity;
import org.andengine.entity.modifier.FadeOutModifier;
import org.andengine.entity.modifier.IEntityModifier;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.ScaleAtModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.text.Text;
import org.andengine.extension.tmx.TMXTile;
import org.andengine.input.touch.TouchEvent;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.IModifier;
import org.andengine.util.modifier.ease.EaseBackOut;
import org.andengine.util.modifier.ease.EaseLinear;

import java.util.ArrayList;
import java.util.List;

/**
 * Database model representing a Word tile that has been placed on the map.
 */
public class GameTile extends Model implements Builder.BuildStatusUpdateHandler, IOnAreaTouchListener, MapBlockSprite.OnClickListener {

    public ForeignKeyField<GameSession> session; /**< reference to the GameSession this tile is a part of */
    public ForeignKeyField<GameTileBuilder> builder; /**< reference to the GameTileBuilder used by this tile */
    public ForeignKeyField<GameTileTimer> timer; /**< the Builder used to track timeouts between game play */
    public IntegerField isoX; /**< isometric X coordinate for this tile */
    public IntegerField isoY; /**< isometric Y coordinate for this tile */
    public CharField item_name; /**< name of the InventoryItem this tile produces */

    public PhoeniciaGame phoeniciaGame; /**< active game instance this tile is a part of */
    public Game game; /**< locale Game this tile represents */
    public PlacedBlockSprite sprite;  /**< sprite that has been placed on the map for this tile */

    private boolean isTouchDown = false;
    private GameTileListener eventListener;
    private boolean isReady= false;

    public GameTile() {
        super();
        this.session = new ForeignKeyField<GameSession>(GameSession.class);
        this.builder = new ForeignKeyField<GameTileBuilder>(GameTileBuilder.class);
        this.timer = new ForeignKeyField<GameTileTimer>(GameTileTimer.class);
        this.isoX = new IntegerField();
        this.isoY = new IntegerField();
        this.item_name = new CharField(32);
    }

    /**
     * Create a new tile for the given game that is a part of the given game
     * @param phoeniciaGame active game instance this tile is a part of
     * @param game locale Letter this tile represents
     */
    public GameTile(PhoeniciaGame phoeniciaGame, Game game) {
        this();
        this.phoeniciaGame = phoeniciaGame;
        this.game = game;
        this.session.set(phoeniciaGame.session);
        this.item_name.set(game.name);
    }

    public static final QuerySet<GameTile> objects(Context context) {
        return objects(context, GameTile.class);
    }

    /**
     * Get the Sprite that represents this tile on the map
     * @return sprite, or null if it does not have one
     */
    public PlacedBlockSprite getSprite() {

        return this.sprite;
    }

    /**
     * Attach a Sprite from the map to this tile
     * @param sprite sprite that represents this tile on the map
     */
    public void setSprite(PlacedBlockSprite sprite) {
        this.sprite = sprite;
    }

    /**
     * Get the builder instance that produces a Word from this tile
     * @param context ApplicationContext for use in database calls
     * @return the builder instance, or null if it does not have one
     */
    public GameTileBuilder getBuilder(Context context) {
        GameTileBuilder builder = this.builder.get(context);
        if (builder != null) {
            builder.addUpdateHandler(this);
            this.onProgressChanged(builder);
            phoeniciaGame.addBuilder(builder);
        }
        return builder;
    }

    /**
     * Attach a WordBuilder to this tile
     * @param builder used by this tile
     */
    public void setBuilder(GameTileBuilder builder) {
        builder.addUpdateHandler(this);
        this.builder.set(builder);
        this.onProgressChanged(builder);
        if (builder.status.get() == Builder.COMPLETE) {
            this.isReady = true;
        }
    }

    public GameTileTimer getTimer() {
        GameTileTimer timer = this.timer.get(PhoeniciaContext.context);
        if (timer == null) {
            timer = new GameTileTimer();
            timer.game.set(this.phoeniciaGame.session);
            timer.progress.set(0);
            timer.status.set(Builder.NONE);
            this.setTimer(timer);
        }
        timer.time.set(this.game.time);
        timer.save(PhoeniciaContext.context);
        this.phoeniciaGame.addBuilder(timer);
        return timer;
    }

    public void setTimer(GameTileTimer timer) {
        this.timer.set(timer);
        this.save(PhoeniciaContext.context);
        timer.addUpdateHandler(new Builder.BuildStatusUpdateHandler() {
            @Override
            public void onScheduled(Builder buildItem) {

            }

            @Override
            public void onStarted(Builder buildItem) {
                isReady = false;
            }

            @Override
            public void onCompleted(Builder buildItem) {
                isReady = true;
            }

            @Override
            public void onProgressChanged(Builder builtItem) {

            }
        });
    }

    /**
     * Restart the build progress for this tile
     * @param context ApplicationContext ApplicationContext for use in database calls
     */
    public void reset(Context context) {
        GameTileTimer timer = this.getTimer();
        if (timer != null) {
            timer.progress.set(0);
            timer.start();
            timer.save(PhoeniciaContext.context);
            this.phoeniciaGame.addBuilder(timer);
        } else {
            Debug.e("Could not reset GameTile timer, because it was missing");
        }
    }


    public void restart(Context context) {
        Debug.d("Restarting game timout");
        GameTileTimer timer = this.getTimer();
        if (timer.status.get() == Builder.COMPLETE) {
            this.isReady = true;
        } else if (timer.status.get() == Builder.BUILDING) {
            this.isReady = false;
        }
    }

    public void onScheduled(Builder buildItem) { Debug.d("WordTile.onScheduled"); this.isReady = false; return; }
    public void onStarted(Builder buildItem) { Debug.d("WordTile.onStarted"); this.isReady = false; return; }
    public void onCompleted(Builder buildItem) {
        Debug.d("WordTile.onCompleted");
        this.isReady = true;
        if (this.eventListener != null) {
            this.eventListener.onGameTileBuildCompleted(this);
        }
        Assets.getInsance().addGameTile(this);
        return;
    }

    /**
     * Called when the builder for this tile gets updated.
     * @param builtItem
     */
    public void onProgressChanged(Builder builtItem) {
        if (sprite != null) {
            sprite.setProgress(builtItem.progress.get(), game.construct);
        }
        if (builtItem.progress.get() >= game.construct) {
            builtItem.complete();
        }
        return;
    }

    @Override
    protected void migrate(Context context) {
        Migrator<GameTile> migrator = new Migrator<GameTile>(GameTile.class);

        migrator.addField("timer", new ForeignKeyField<GameTileTimer>(GameTileTimer.class));
        // roll out all migrations
        migrator.migrate(context);
        return;
    }

    public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown()) {
            this.isTouchDown = true;
        } else if (isTouchDown && pSceneTouchEvent.isActionUp()) {

            if (this.eventListener != null) {
                this.eventListener.onGameTileClicked(this);
            }
            this.isTouchDown = false;
            return true;
        } else {
            this.isTouchDown = false;
        }
        return false;

    }

    /**
     * Called when the Sprite for this tile is clicked by the player
     * @param buttonSprite
     * @param v
     * @param v2
     */
    public void onClick(MapBlockSprite buttonSprite, float v, float v2) {
        Debug.d("Clicked block: "+String.valueOf(this.game.name));
        Builder builder = this.getBuilder(PhoeniciaContext.context);
        Builder timer = this.getTimer();
        if (builder != null && timer != null) {
            if (builder.status.get() == Builder.COMPLETE && (timer.status.get() == Builder.COMPLETE || timer.status.get() == Builder.NONE)) {
                phoeniciaGame.hudManager.showGame(phoeniciaGame.locale.level_map.get(phoeniciaGame.current_level), this);
            } else {
                Debug.d("Clicked block was NOT ready");
                // Don't run another modifier animation if one is still running
                String progress;
                if (builder.status.get() != Builder.COMPLETE) {
                    progress = String.valueOf(100 * builder.progress.get() / builder.time.get());
                } else {
                    progress = String.valueOf(100 * timer.progress.get() / timer.time.get());
                }
                if (sprite.getEntityModifierCount() <= 0) {
                    sprite.registerEntityModifier(new ScaleAtModifier(0.5f, sprite.getScaleX(), sprite.getScaleX(), sprite.getScaleY() * 0.7f, sprite.getScaleY(), sprite.getScaleCenterX(), 0, EaseBackOut.getInstance()));

                    int time_left;
                    if (builder.status.get() != Builder.COMPLETE) {
                        time_left = builder.time.get() - builder.progress.get();
                    } else {
                        time_left = timer.time.get() - timer.progress.get();
                    }
                    String time_display = String.valueOf(time_left) + "s";
                    if (time_left > (60*60)) {
                        time_left = time_left / (60*60);
                        time_display = String.valueOf(time_left) + "h";
                    } else if (time_left > 60) {
                        time_left = time_left / 60;
                        time_display = String.valueOf(time_left) + "m";
                    }
                    final Text progressText = new Text(sprite.getWidth()/2, 16, GameFonts.progressText(), time_display, time_display.length(), PhoeniciaContext.vboManager);
                    sprite.attachChild(progressText);
                    progressText.registerEntityModifier(new ParallelEntityModifier(
                            new ScaleModifier(0.4f, 0.3f, 0.8f, EaseLinear.getInstance()),
                            new FadeOutModifier(3.0f, new IEntityModifier.IEntityModifierListener() {
                                @Override
                                public void onModifierStarted(IModifier<IEntity> iModifier, IEntity iEntity) {
                                }

                                @Override
                                public void onModifierFinished(IModifier<IEntity> iModifier, IEntity iEntity) {
                                    phoeniciaGame.activity.runOnUpdateThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sprite.detachChild(progressText);
                                        }
                                    });
                                }
                            }, EaseLinear.getInstance())
                    ));
                }
                //phoeniciaGame.playBlockSound(this.game.sound);
            }
        } else {
            Debug.e("Clicked block has no builder");
        }
    }

    @Override
    public void onHold(MapBlockSprite buttonSprite, float v, float v2) {
        final TMXTile tmxTile = phoeniciaGame.getTileAtIso(this.isoX.get(), this.isoY.get());
        if (tmxTile == null) {
            Debug.d("No tile at "+this.isoX.get()+"x"+this.isoY.get());
            return;
        }
        phoeniciaGame.hudManager.push(new SpriteMoveHUD(phoeniciaGame, tmxTile, sprite, game.columns, game.rows, this.game.restriction, new SpriteMoveHUD.SpriteMoveHandler() {
            @Override
            public void onSpriteMoveCanceled(MapBlockSprite sprite) {
                float[] oldPos = GameTextures.calculateTilePosition(tmxTile, sprite, game.columns, game.rows);
                sprite.setPosition(oldPos[0], oldPos[1]);
                sprite.setZIndex(tmxTile.getTileZ());
                phoeniciaGame.scene.sortChildren();
            }

            @Override
            public void onSpriteMoveFinished(MapBlockSprite sprite, TMXTile newlocation) {
                isoX.set(newlocation.getTileColumn());
                isoY.set(newlocation.getTileRow());
                // Unset previous sprite location
                for (int c = 0; c < game.columns; c++) {
                    for (int r = 0; r < game.rows; r++) {
                        phoeniciaGame.placedSprites[tmxTile.getTileColumn()-c][tmxTile.getTileRow()-r] = null;
                    }
                }
                // Set new sprite location
                for (int c = 0; c < game.columns; c++) {
                    for (int r = 0; r < game.rows; r++) {
                        phoeniciaGame.placedSprites[newlocation.getTileColumn()-c][newlocation.getTileRow()-r] = sprite;
                    }
                }
                sprite.setZIndex(newlocation.getTileZ());
                phoeniciaGame.scene.sortChildren();
                save(PhoeniciaContext.context);
            }
        }));

    }

    public void setListener(final GameTileListener listener) {
        this.eventListener = listener;
    }

    /**
     * Callback handler for listening to changes and events on this tile
     */
    public interface GameTileListener {
        public void onGameTileClicked(final GameTile wordTile);
        public void onGameTileBuildCompleted(final GameTile wordTile);
    }
}

