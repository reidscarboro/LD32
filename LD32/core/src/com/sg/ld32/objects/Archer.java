package com.sg.ld32.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.sg.ld32.LD32Main;
import com.sg.ld32.screens.GameScreen;
import com.sg.ld32.utilities.Values;

public class Archer implements Drawable, PhysicsObject, Updatable, Comparable<Drawable>, Swappable, Killable{
	
	public Sprite sprite_0;
	public Sprite sprite_1;
	public Sprite sprite_2;
	public Body body;
	public float angle;
	
	private GameScreen gameScreen;
	private Vector2 moveForce;
	
	private boolean swapWaiting = false;
	private Vector2 swapLocation;
	
	private Vector2 arrowFireLocation;
	
	boolean awake = false;
	
	boolean dyingFire = false;
	boolean dyingFall = false;
	boolean dyingBleed = false;
	int deathTimer = 0;
	
	private int cooldownTimer = 0;
	
	@Override
	public int compareTo(Drawable o) {
		if (this.getY() == o.getY() ){
			return Float.compare(this.getX(), o.getX());
		} else {
			return -Float.compare(this.getY(), o.getY());
		}
	}
	
	
	public Archer(GameScreen gameScreen, int x, int y){
		this.gameScreen = gameScreen;
		
		arrowFireLocation = new Vector2(1,1);
		
		moveForce = new Vector2(Values.ARCHER_MOVE_FORCE, Values.ARCHER_MOVE_FORCE);
		
		BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
       
        if (!gameScreen.world.isLocked()) body = gameScreen.world.createBody(bodyDef);
       

        CircleShape shape = new CircleShape();
        shape.setRadius(0.40f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;

        Fixture fixture = getBody().createFixture(fixtureDef);
        Map<String, Object> userData = new HashMap<String, Object>();
        userData.put("type", "Archer");
        userData.put("parent", this);
        fixture.setUserData(userData);

        shape.dispose();
        
        getBody().setFixedRotation(true);
        
		
		sprite_0 = new Sprite(LD32Main.assetManager.get("archer_0.png", Texture.class));
		sprite_0.setSize(1f, 1f);
		sprite_1 = new Sprite(LD32Main.assetManager.get("archer_1.png", Texture.class));
		sprite_1.setSize(1f, 1f);
		sprite_2 = new Sprite(LD32Main.assetManager.get("archer_2.png", Texture.class));
		sprite_2.setSize(1f, 1f);
		
		Random rng = new Random();
		angle = rng.nextInt(360);
	}

	@Override
	public void update() {
		if (dyingFall || dyingBleed || dyingFire){
			MassData deadMassData = new MassData();
			getBody().setLinearVelocity(0, 0);
			getBody().setMassData(deadMassData);
			deathTimer++;
			if (deathTimer > Values.ZOMBIE_DEATH_TIME){
				if (!gameScreen.world.isLocked() && getBody() != null) gameScreen.world.destroyBody(getBody());
			}
			
			if (sprite_0.getColor().a > 0.2f){
				sprite_0.setColor(1, 1, 1, sprite_0.getColor().a - 0.06f);
				sprite_1.setColor(1, 1, 1, sprite_0.getColor().a - 0.06f);
				sprite_2.setColor(1, 1, 1, sprite_0.getColor().a - 0.06f);
			} else {
				sprite_0.setColor(1, 1, 1, 0);
				sprite_1.setColor(1, 1, 1, 0);
				sprite_2.setColor(1, 1, 1, 0);
			}
			
			if (dyingFall){
				float spriteScale = -0.01f;
				if (sprite_0.getScaleX() > 0){
					sprite_0.scale(spriteScale);
					sprite_1.scale(spriteScale);
					sprite_2.scale(spriteScale);
				}
			}
		} else {
			if (swapWaiting){
				getBody().setTransform(swapLocation, getBody().getAngle());
				swapWaiting = false;
			}
			
			if (awake){
				//walk towards player or something
				Vector2 vecToPlayer = gameScreen.player.getBody().getPosition().sub(getBody().getPosition());
				angle = vecToPlayer.angle();
				moveForce.setAngle(angle + 180);
				
				if (vecToPlayer.len() < Values.ARCHER_RETREAT_DISTANCE){
					getBody().applyForceToCenter(moveForce, true);
					
					if (getBody().getLinearVelocity().len() > Values.ARCHER_MAX_SPEED){
						getBody().setLinearVelocity(getBody().getLinearVelocity().nor().scl(Values.ARCHER_MAX_SPEED));
					}
				} else {
					getBody().setLinearVelocity(0,0);
				}
				
				Random rng = new Random();
				if (cooldownTimer < Values.ARCHER_BULLET_COOLDOWN){
					cooldownTimer++;
				} else {
					if (rng.nextFloat() > 0.99f){
						shoot(angle - 15 + rng.nextInt(30) + 1);
						cooldownTimer = 0;
					}
				}
			} else {
				if (gameScreen.player.getBody().getPosition().sub(getBody().getPosition()).len() < Values.ARCHER_WAKE_DISTANCE){
					awake = true;
				}
			}
		}
	}

	@Override
	public Body getBody() {
		return body;
	}

	@Override
	public void draw(SpriteBatch spriteBatch) {
		sprite_0.setOriginCenter();
		sprite_0.setRotation(angle + 90);
		sprite_1.setOriginCenter();
		sprite_1.setRotation(angle + 90);
		sprite_2.setOriginCenter();
		sprite_2.setRotation(angle + 90);
		sprite_0.setPosition(getBody().getPosition().x - 0.5f, getBody().getPosition().y - 0.5f - (1-sprite_0.getScaleX()));
		sprite_0.draw(spriteBatch);
		
		sprite_1.setPosition(getBody().getPosition().x - 0.5f, getBody().getPosition().y - 0.5f + 0.2f - (1-sprite_0.getScaleX()));
		sprite_1.draw(spriteBatch);
		
		sprite_2.setPosition(getBody().getPosition().x - 0.5f, getBody().getPosition().y - 0.5f + 0.4f - (1-sprite_0.getScaleX()));
		sprite_2.draw(spriteBatch);
	}

	@Override
	public float getX() {
		return getBody().getPosition().x;
	}

	@Override
	public float getY() {
		return getBody().getPosition().y;
	}


	@Override
	public void swapTo(Vector2 position) {
		swapWaiting = true;
		swapLocation = position;
		awake = true;
	}


	@Override
	public Vector2 getPosition() {
		return getBody().getPosition();
	}


	@Override
	public void killFire() {
		dyingFire = true;
	}

	@Override
	public void killFall() {
		dyingFall = true;
	}

	@Override
	public void killBleed() {
		dyingBleed = true;
	}


	@Override
	public void checkFall(){
		boolean grounded = false;
		
		Vector2 bottomLeft = getPosition().cpy().add(-0.4f, -0.4f);
		bottomLeft.x = (int) Math.round(bottomLeft.x);
		bottomLeft.y = (int) Math.round(bottomLeft.y);
		if (gameScreen.floors.containsKey(bottomLeft)) grounded = true;
		
		Vector2 bottomRight = getPosition().cpy().add(0.4f, -0.4f);
		bottomRight.x = (int) Math.round(bottomRight.x);
		bottomRight.y = (int) Math.round(bottomRight.y);
		if (gameScreen.floors.containsKey(bottomRight)) grounded = true;
		
		Vector2 topLeft = getPosition().cpy().add(-0.4f, 0.4f);
		topLeft.x = (int) Math.round(topLeft.x);
		topLeft.y = (int) Math.round(topLeft.y);
		if (gameScreen.floors.containsKey(topLeft)) grounded = true;
		
		Vector2 topRight = getPosition().cpy().add(0.4f, 0.4f);
		topRight.x = (int) Math.round(topRight.x);
		topRight.y = (int) Math.round(topRight.y);
		if (gameScreen.floors.containsKey(topRight)) grounded = true;
		
		if (!grounded){
			killFall();
		}
	}
	
	@Override
	public boolean isDeathComplete(){
		return deathTimer > Values.ARCHER_DEATH_TIME;
	}
	
	public void shoot(float angle){
		Vector2 location = getBody().getPosition().cpy();
		arrowFireLocation.setAngle(angle);
		location.add(arrowFireLocation);
		gameScreen.addArrow(new Arrow(gameScreen, location.x, location.y, angle));
	}
}
