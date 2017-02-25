package fr.glav.shmup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.System.currentTimeMillis;

/**
 * Created by Cyril on 22/02/2017.
 */

public class ShmupView extends View implements SensorEventListener
{
	private TextPaint textPaint;

	public static int screenWidth, screenHeight;
	private float sensorY, sensorX;

	static final int
		STATUS_GAME_PLAY = 1,
		STATUS_GAME_OVER = 2;
	private int status = STATUS_GAME_PLAY;

	private Player player;
	private ArrayList<Bullet> bullets;
	private ArrayList<Enemy> enemies;
	private long enemySpawnTimer;
	public static int score;

	public ShmupView(Context context)
	{
		super(context);
		init(context);
	}

	public ShmupView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}

	private void init(Context context)
	{
		textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

		screenWidth = getScreenMetrics(context).widthPixels;
		screenHeight = getScreenMetrics(context).heightPixels;

		// Touch to shoot bullets
		setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(status == STATUS_GAME_PLAY) {
					bullets.add(new Bullet(player.x, player.y));

				}
				return false;
			}
		});
		
		// Accelerometer
		SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

		resetGame();
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;

		sensorX = event.values[1];
		sensorY = event.values[0];
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	private void resetGame()
	{
		status = STATUS_GAME_PLAY;

		player = new Player(screenWidth - 50, screenHeight / 2);
		bullets = new ArrayList<Bullet>();
		enemies = new ArrayList<Enemy>();

		score = 0;
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);
		invalidate();

		// Game Over
		if(status == STATUS_GAME_OVER)
		{
			writeText(canvas, "Game Over", screenWidth/2, screenHeight/2, 50, Color.GRAY);
		}

		// Score
		writeText(canvas, String.valueOf(score), screenWidth/2, screenHeight-10, 25, Color.RED);

		{ // Player
		if (status == STATUS_GAME_PLAY) {
			player.vx = sensorX * 1.5f;
			player.vy = sensorY * 1.5f;
		} else {
			player.vx = player.vy = 0;
		}
		player.update();
		player.draw(canvas);
		}

		{ // Bullets
		Iterator<Bullet> i = bullets.iterator();
		while (i.hasNext()) {
			Bullet bullet = i.next();

			bullet.update();
			bullet.draw(canvas);

			// Check if bullet is out of screen
			if (bullet.x < 0) {
				i.remove();
				continue;
			}
		}
		}

		{ // Enemies
		Iterator<Enemy> i = enemies.iterator();
		while (i.hasNext()) {
			Enemy enemy = i.next();

			enemy.draw(canvas);

			// Check for collisions with player
			if (player.collidesWith(enemy)) {
				status = STATUS_GAME_OVER;
				return;
			}

			enemy.update();

			// Check if enemy is out of screen
			if (enemy.x > screenWidth) {
				i.remove();
				continue;
			}

			// Check for collisions with bullets
			for (Bullet bullet : bullets) {
				if (bullet.collidesWith(enemy)) {
					i.remove();
					bullets.remove(bullet);
					score++;
					break;
				}
			}
		}
		}

		// Spawn enemies
		if(currentTimeMillis() - enemySpawnTimer > 1000 / (1+score*score/1000))
		{
			enemySpawnTimer = currentTimeMillis();
			enemies.add(new Enemy());
		}
	}

	private void writeText(Canvas canvas, String text, float x, float y, int size, int color) {
		textPaint.setColor(color);
		textPaint.setTextSize(size);
		textPaint.setTextAlign(Paint.Align.CENTER);
		canvas.drawText(text, x, y, textPaint);
	}

	private DisplayMetrics getScreenMetrics(Context context) {
		WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(dm);
		return dm;
	}
}

abstract class Thing
{
	protected Paint paint;
	public float x, y, vx, vy;

	public Thing()
	{
		this(0, 0);
	}
	public Thing(float x, float y)
	{
		this.x = x;
		this.y = y;
		this.vx = 0;
		this.vy = 0;
		init();
	}

	protected void init()
	{
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setStrokeWidth(1.f);
		paint.setColor(Color.BLACK);
	}

	public void update()
	{
		x += vx;
		y += vy;
	}

	public abstract void draw(Canvas canvas);

	public boolean collidesWith(Thing other)
	{
		return distanceWith(other) < 40;
	}

	public float distanceWith(Thing other)
	{
		return (float) Math.sqrt(
			Math.pow((x - other.x), 2) +
			Math.pow((y - other.y), 2)
		);
	}
}

class Player extends Thing
{
	public Player(float x, float y)
	{
		super(x, y);
	}

	@Override
	protected void init()
	{
		super.init();
		paint.setColor(Color.RED);
	}

	@Override
	public void draw(Canvas canvas)
	{
		Path triangle = new Path();
		triangle.moveTo(x+20, y-20);
		triangle.lineTo(x-20, y);
		triangle.lineTo(x+20, y+20);
		triangle.lineTo(x+10, y);
		triangle.close();
		canvas.drawPath(triangle, paint);
	}

	@Override
	public void update()
	{
		super.update();
		// Boundaries
		x = Math.max(x, 0);
		y = Math.max(y, 0);
		x = Math.min(x, ShmupView.screenWidth);
		y = Math.min(y, ShmupView.screenHeight);
	}
}

class Enemy extends Thing
{
	@Override
	protected void init()
	{
		super.init();
		x = 0;
		y = (float) Math.random() * ShmupView.screenHeight;
		vx = 2 + ShmupView.score*0.1f;
	}

	@Override
	public void update()
	{
		vy += (float) (Math.random() * 0.5) - 0.25;
		super.update();
	}

	@Override
	public void draw(Canvas canvas)
	{
		canvas.drawRect(x-20, y-20, x+20, y+20, paint);
	}
}

class Bullet extends Thing
{
	public Bullet(float x, float y)
{
	super(x, y);
}

	@Override
	protected void init()
	{
		super.init();
		vx = -10;
		paint.setColor(Color.RED);
	}

	@Override
	public void draw(Canvas canvas)
	{
		canvas.drawRect(x-10, y-3, x+10, y+3, paint);
	}
}
