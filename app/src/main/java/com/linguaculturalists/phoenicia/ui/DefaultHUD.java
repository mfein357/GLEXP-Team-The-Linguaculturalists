package com.linguaculturalists.phoenicia.ui;

import com.linguaculturalists.phoenicia.GameActivity;
import com.linguaculturalists.phoenicia.PhoeniciaGame;
import com.linguaculturalists.phoenicia.components.ToggleSprite;
import com.linguaculturalists.phoenicia.locale.Game;
import com.linguaculturalists.phoenicia.locale.Level;
import com.linguaculturalists.phoenicia.models.Bank;
import com.linguaculturalists.phoenicia.models.GameSession;
import com.linguaculturalists.phoenicia.tour.TourOverlay;
import com.linguaculturalists.phoenicia.util.GameFonts;
import com.linguaculturalists.phoenicia.util.GameTextures;
import com.linguaculturalists.phoenicia.util.GameUI;
import com.linguaculturalists.phoenicia.util.PhoeniciaContext;
import com.linguaculturalists.phoenicia.util.RepeatedClickDetectorListener;

import org.andengine.entity.Entity;
import org.andengine.entity.modifier.MoveYModifier;
import org.andengine.entity.sprite.ButtonSprite;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.input.touch.TouchEvent;
import org.andengine.input.touch.detector.ClickDetector;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.texture.region.ITiledTextureRegion;
import org.andengine.util.adt.align.HorizontalAlign;
import org.andengine.util.debug.Debug;
import org.andengine.util.modifier.ease.EaseBackOut;

/**
 * The default HUD for Phoenicia
 *
 * Displays the current level, bank account balance, and buttons for adding letter or work tiles
 */
public class DefaultHUD extends PhoeniciaHUD implements PhoeniciaGame.LevelChangeListener, Bank.BankUpdateListener {

    private ButtonSprite levelIcon;
    private Text levelDisplay;
    private Sprite coinIcon;
    private Text balanceDisplay;
    private ButtonSprite helpButton;
    private ToggleSprite musicButton;
    //private ButtonSprite inventoryBlock;
    private ButtonSprite letterBlock;
    private ButtonSprite wordBlock;
    private ButtonSprite gameBlock;
    private ButtonSprite decorationBlock;

    private ClickDetector debugClickDetector;
    /**
     * Displays the current level, bank account balance, and buttons for adding letter or work tiles
     * @param game Reference to the PhoeniciaGame this HUD is running in
     */
    public DefaultHUD(final PhoeniciaGame game) {
        super(game);
        this.setBackgroundEnabled(false);
        this.game = game;
        this.game.addLevelListener(this);
        Bank.getInstance().addUpdateListener(this);


        ITextureRegion levelRegion = GameUI.getInstance().getLevelDisplay();
        levelIcon = new ButtonSprite(levelRegion.getWidth()/2, GameActivity.CAMERA_HEIGHT-(levelRegion.getHeight()/2), levelRegion, PhoeniciaContext.vboManager);
        levelIcon.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v1) {
                //game.hudManager.showNewLevel(game.getCurrentLevel(), false);
            }
        });
        levelDisplay = new Text(160, levelIcon.getHeight()/2, GameFonts.defaultHUDDisplay(), game.current_level, 20, new TextOptions(HorizontalAlign.LEFT), PhoeniciaContext.vboManager);
        levelIcon.attachChild(levelDisplay);
        this.attachChild(levelIcon);
        this.registerTouchArea(levelIcon);

        ITextureRegion coinRegion = GameUI.getInstance().getCoinsDisplay();
        coinIcon = new Sprite(coinRegion.getWidth()/2, GameActivity.CAMERA_HEIGHT-(coinRegion.getHeight()/2)-64, coinRegion, PhoeniciaContext.vboManager);
        balanceDisplay = new Text(160, levelRegion.getHeight()/2, GameFonts.defaultHUDDisplay(), game.session.account_balance.get().toString(), 20, new TextOptions(HorizontalAlign.LEFT), PhoeniciaContext.vboManager);
        coinIcon.attachChild(balanceDisplay);
        this.attachChild(coinIcon);


        this.debugClickDetector = new ClickDetector(new RepeatedClickDetectorListener(10, 5*1000) {

            @Override
            public void onRepeatedClick(ClickDetector clickDetector, int i, float v, float v1) {
                game.hudManager.showDebugMode();
            }

        });
        Entity debugTouchArea = new Entity(50, this.coinIcon.getY() + this.levelIcon.getHeight()/3, 100, 100) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                debugClickDetector.onManagedTouchEvent(pSceneTouchEvent);
                return false;
            }
        };
        this.attachChild(debugTouchArea);
        this.registerTouchArea(debugTouchArea);

        ITextureRegion helpRegion = GameUI.getInstance().getHelpIcon();
        this.helpButton = new ButtonSprite(GameActivity.CAMERA_WIDTH-(helpRegion.getWidth()/2), GameActivity.CAMERA_HEIGHT-(helpRegion.getHeight()/2), helpRegion, PhoeniciaContext.vboManager);
        helpButton.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                game.hudManager.showNextLevelReq(game.getCurrentLevel());
//                Level level = game.locale.level_map.get(game.current_level);
//                while (level.intro.size() < 1 && level.prev != null && level.prev != level) {
//                    level = level.prev;
//                }
//                if (level.intro.size() > 0) {
//                    game.hudManager.showLevelIntro(level);
//                }
            }
        });
        this.registerTouchArea(helpButton);
        this.attachChild(helpButton);

        ITiledTextureRegion musicRegion = GameUI.getInstance().getMusicIcon();
        this.musicButton = new ToggleSprite(helpButton.getX()-(musicRegion.getWidth()/4)*3 - 16, GameActivity.CAMERA_HEIGHT-(musicRegion.getHeight()/2), musicRegion, PhoeniciaContext.vboManager) {
            @Override
            public void onToggled() {
                if (this.isEnabled()) {
                    game.music.resume();
                    game.session.pref_music.set(true);
                } else {
                    game.music.pause();
                    game.session.pref_music.set(false);
                }
                game.session.save(PhoeniciaContext.context);
            }
        };
        musicButton.setEnabled(game.session.pref_music.get());
        this.registerTouchArea(musicButton);
        this.attachChild(musicButton);

        ITextureRegion decorationRegion = GameUI.getInstance().getDecorationLauncher();
        this.decorationBlock = new ButtonSprite(16 + decorationRegion.getWidth() / 2, (decorationRegion.getHeight() / 2) + 16, decorationRegion, PhoeniciaContext.vboManager);
        decorationBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                game.hudManager.showDecorationPlacement();
            }
        });
        this.registerTouchArea(decorationBlock);

        if (this.game.getCurrentLevel().decorations.size() > 0) {
            Debug.d("Found "+this.game.getCurrentLevel().decorations.size()+" available decorations");
            this.attachChild(decorationBlock);
        }

        int position = 1;
        ITextureRegion gameRegion = GameUI.getInstance().getGameLauncher();
        this.gameBlock = new ButtonSprite(16 + GameActivity.CAMERA_WIDTH - (gameRegion.getWidth() + 16) * position, (gameRegion.getHeight() / 2) + 16, gameRegion, PhoeniciaContext.vboManager);
        gameBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                game.hudManager.showGamePlacement();
            }
        });
        this.registerTouchArea(gameBlock);
        if (this.game.getCurrentLevel().games.size() > 0) {
            Debug.d("Found "+this.game.getCurrentLevel().games.size()+" available games");
            this.attachChild(gameBlock);
            position++;
        }

        ITextureRegion wordRegion = GameUI.getInstance().getWordLauncher();
        this.wordBlock = new ButtonSprite(16 + GameActivity.CAMERA_WIDTH - (wordRegion.getWidth() + 16) * position, (wordRegion.getHeight() / 2) + 16, wordRegion, PhoeniciaContext.vboManager);
        wordBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                game.hudManager.showWordPlacement();
            }
        });
        this.registerTouchArea(wordBlock);
        if (this.game.getCurrentLevel().words.size() > 0) {
            this.attachChild(wordBlock);
            position++;
        }

        ITextureRegion letterRegion = GameUI.getInstance().getLetterLauncher();
        this.letterBlock = new ButtonSprite(16+GameActivity.CAMERA_WIDTH-(letterRegion.getWidth()+16) * position, (letterRegion.getHeight()/2)+16, letterRegion, PhoeniciaContext.vboManager);
        letterBlock.setOnClickListener(new ButtonSprite.OnClickListener() {
            @Override
            public void onClick(ButtonSprite buttonSprite, float v, float v2) {
                game.hudManager.showLetterPlacement();
            }
        });
        this.registerTouchArea(letterBlock);
        this.attachChild(letterBlock);

    }

    /**
     * Animate the on-screen elements entering the scene
     */
    @Override
    public void show() {
        //inventoryBlock.registerEntityModifier(new MoveYModifier(0.5f, -48, 64, EaseBackOut.getInstance()));
        letterBlock.registerEntityModifier(new MoveYModifier(0.5f, -(letterBlock.getHeight()/2), (letterBlock.getHeight()/2)+16, EaseBackOut.getInstance()));
        wordBlock.registerEntityModifier(new MoveYModifier(0.5f, -(wordBlock.getHeight()/2), (wordBlock.getHeight()/2)+16, EaseBackOut.getInstance()));
        gameBlock.registerEntityModifier(new MoveYModifier(0.5f, -(gameBlock.getHeight()/2), (gameBlock.getHeight()/2)+16, EaseBackOut.getInstance()));
        decorationBlock.registerEntityModifier(new MoveYModifier(0.5f, -(decorationBlock.getHeight()/2), (decorationBlock.getHeight()/2)+16, EaseBackOut.getInstance()));

        helpButton.registerEntityModifier(new MoveYModifier(0.5f, GameActivity.CAMERA_HEIGHT + 48, GameActivity.CAMERA_HEIGHT-(helpButton.getHeight()/2), EaseBackOut.getInstance()));
        musicButton.registerEntityModifier(new MoveYModifier(0.5f, GameActivity.CAMERA_HEIGHT + 48, GameActivity.CAMERA_HEIGHT-(musicButton.getHeight()/2), EaseBackOut.getInstance()));

        levelIcon.registerEntityModifier(new MoveYModifier(0.5f, GameActivity.CAMERA_HEIGHT + 48, GameActivity.CAMERA_HEIGHT-(levelIcon.getHeight()/2), EaseBackOut.getInstance()));
        //levelDisplay.registerEntityModifier(new MoveYModifier(0.5f, GameActivity.CAMERA_HEIGHT + 48, GameActivity.CAMERA_HEIGHT - 24, EaseBackOut.getInstance()));

        coinIcon.registerEntityModifier(new MoveYModifier(0.5f, GameActivity.CAMERA_HEIGHT - 16, GameActivity.CAMERA_HEIGHT-(coinIcon.getHeight()/2)-64, EaseBackOut.getInstance()));
        //balanceDisplay.registerEntityModifier(new MoveYModifier(0.5f, GameActivity.CAMERA_HEIGHT - 52, GameActivity.CAMERA_HEIGHT - 104, EaseBackOut.getInstance()));

    }

    /**
     * Move the on-screen elements offscreen for later animation coming it
     */
    @Override
    public void hide() {
        //inventoryBlock.setY(-48);
        letterBlock.setY(-48);
        wordBlock.setY(-48);
        gameBlock.setY(-48);
        //levelDisplay.setY(GameActivity.CAMERA_HEIGHT + 48);
        //balanceDisplay.setY(GameActivity.CAMERA_HEIGHT + 16);
        helpButton.setY(GameActivity.CAMERA_HEIGHT + 32);
        musicButton.setY(GameActivity.CAMERA_HEIGHT + 32);
    }

    /**
     * Called when the game's level has changed
     * @param next the new level being started
     */
    public void onLevelChanged(Level next) {
        this.levelDisplay.setText(next.name);
        //this.levelDisplay.setPosition(64 + (this.levelDisplay.getWidth() / 2), this.levelDisplay.getY());
        if (this.game.getCurrentLevel().decorations.size() > 0) {
            if (!this.decorationBlock.hasParent()) this.attachChild(this.decorationBlock);
        }
        int position = 1;
        if (this.game.getCurrentLevel().games.size() > 0) {
            this.gameBlock.setX(16 + GameActivity.CAMERA_WIDTH - (this.gameBlock.getWidth() + 16) * position);
            if (!this.gameBlock.hasParent()) this.attachChild(this.gameBlock);
            position++;
        }
        if (this.game.getCurrentLevel().words.size() > 0) {
            this.wordBlock.setX(16 + GameActivity.CAMERA_WIDTH - (this.wordBlock.getWidth() + 16) * position);
            if (!this.wordBlock.hasParent()) this.attachChild(this.wordBlock);
            position++;
        }
        this.letterBlock.setX(16 + GameActivity.CAMERA_WIDTH - (this.letterBlock.getWidth() + 16) * position);
    }

    /**
     * Called when the player's bank account balance changes
     * @param new_balance the player's new account balance
     */
    public void onBankAccountUpdated(int new_balance) {
        Debug.d("New balance: " + new_balance);
        this.balanceDisplay.setText(""+new_balance);
        //this.balanceDisplay.setPosition(64 + (this.balanceDisplay.getWidth() / 2), this.balanceDisplay.getY());
    }

    @Override
    public void finish() {
        // Default HUD should never finish
    }
}
