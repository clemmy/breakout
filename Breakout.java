import javax.swing.*;
import java.util.ArrayList;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;

class Breakout {

	View view;
	Splash splashView;
	Model model;
	final int originalScreenWidth = 800;
	final int originalScreenHeight = 600;
	double xScale = 1.0;
	double yScale = 1.0;
	int ballSpeed;
	double pixelsPerFrame;


	Breakout(int ballSpeed, int frameRate) {
		this.ballSpeed = ballSpeed;
		this.pixelsPerFrame = (double) ballSpeed / frameRate;
		this.model = new Model(frameRate);
		this.view = new View(this.model);
		this.splashView = new Splash();
		this.model.addSubscriber(this.view);
	}

	class Block {
		int health;
		int x;
		int y;
		int height;
		int width;

		Block(int x, int y, int width, int height, int health) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.health = health;
		}

		// 0 - vertical, 1 - horizontal
		int getCollisionAxis(Ball ball) {
			Block block = this;

			Boolean outsideVerticals = ((ball.x + ball.diameter) < block.x) || (ball.x > (block.x + block.width));
			Boolean outsideHorizontals = ((ball.y + ball.diameter) < block.y) || (ball.y > (block.y + block.height));
			Boolean isOnTopLine = (ball.y + ball.diameter >= block.y) && (ball.y <= block.y) && !outsideVerticals;
			Boolean isOnBottomLine = (ball.y + ball.diameter >= block.y + block.height) && (ball.y <= block.y + block.height) && !outsideVerticals;
			Boolean isOnLeftLine = (ball.x + ball.diameter >= block.x) && (ball.x <= block.x) && !outsideHorizontals;
			Boolean isOnRightLine = (ball.x + ball.diameter >= block.x + block.width) && (ball.x <= block.x + block.width) && !outsideHorizontals;

			if (isOnTopLine || isOnBottomLine) {
				return 1; // horizontal bounce
			} else if (isOnRightLine || isOnLeftLine) {
				return 0; // vertical bounce
			} else {
				return -1;
			}
		}

		void moveDown() {
			this.y += this.height+2;
		}

		void moveUp() {
			this.y -= this.height*6+12;
		}

		void paint(Graphics2D g2d) {
			g2d.setColor(Color.GREEN);
			g2d.fill(this.getScaledRoundRectangle());
		}

		RoundRectangle2D.Double getScaledRoundRectangle() {
			return new RoundRectangle2D.Double((int)(this.x*xScale), (int)(this.y*yScale), (int)(this.width*xScale), (int)(this.height*yScale), 5, 5);
		}

		Rectangle2D.Double getRectangle() {
			return new Rectangle2D.Double(this.x, this.y, this.width, this.height);
		}
	}

	class Ball {
		double x;
		double y;
		double diameter;
		Boolean moveRight = false;
		Boolean moveLeft = false;
		Boolean launched = false;
		int direction_x;
		int direction_y;

		Ball(int x, int y, int diameter) {
			this.x = x;
			this.y = y;
			this.diameter = diameter;
		}

		void launch(int paddleMovement) {
			this.launched = true;
			if (paddleMovement == -1) { // left
				this.direction_x = -1;
				this.direction_y = -1;
			} else if (paddleMovement == 0) { // still
				this.direction_x = 0;
				this.direction_y = -1;
			} else if (paddleMovement == 1) { // right
				this.direction_x = 1;
				this.direction_y = -1;
			} else {
				throw new IllegalArgumentException("Invalid paddleMovement value");
			}
		}

		void bounceHorizontal() {
			this.direction_y = this.direction_y * -1;
		}

		void bounceVertical() {
			this.direction_x = this.direction_x * -1;
		}

		void follow(Paddle p) {
			this.x = p.x + p.width/2 - this.diameter/2; // follow
			this.y = p.y - this.diameter;
		}

		void update(Paddle p) {
			if (!this.launched) {
				this.follow(p);
			} else {
				this.x += this.direction_x * (int)pixelsPerFrame;
				this.y += this.direction_y * (int)pixelsPerFrame;
			}
		}

		void paint(Graphics2D g2d) {
			g2d.setColor(Color.YELLOW);
			g2d.fill(this.getScaledEllipse());
		}

		Ellipse2D.Double getEllipse() {
			return new Ellipse2D.Double(this.x, this.y, this.diameter, this.diameter);
		}

		Ellipse2D.Double getScaledEllipse() {
			return new Ellipse2D.Double((int)(this.x*xScale), (int)(this.y*yScale), (int)(this.diameter*xScale), (int)(this.diameter*yScale));
		}
	}

	class Paddle {
		int x;
		int y;
		int height;
		int width;
		Boolean moveLeft = false;
		Boolean moveRight = false;

		Paddle(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		RoundRectangle2D.Double getScaledRoundRectangle() {
			return new RoundRectangle2D.Double((int)(this.x*xScale), (int)(this.y*yScale), (int)(this.width*xScale), (int)(this.height*yScale), 10, 10);
		}

		Rectangle2D.Double getRectangle() {
			return new Rectangle2D.Double(this.x, this.y, this.width, this.height);
		}

		void update() {
			if (this.x > -5 && this.moveLeft) {
				this.x -= (int) (pixelsPerFrame * 1.5);
			} else if (this.x + this.width < originalScreenWidth + 5 && this.moveRight) {
				this.x += (int) (pixelsPerFrame * 1.5);
			}
		}

		void paint(Graphics2D g2d) {
			g2d.setColor(Color.RED);
			g2d.fill(this.getScaledRoundRectangle());
		}
	}

	class Model {
		ArrayList<Block> blocks;
		Ball ball;
		Paddle paddle;
		java.util.Timer timer;
		int frameRate;
		View view;
		Boolean isGameOver = false;
		int playerLives = 3;
		int playerScore = 0;

		Model(int frameRate) {
			this.frameRate = frameRate;

			int blockWidth = 70;
			int blockHeight = 30;
			this.blocks = new ArrayList<Block>();
			for(int x=blockWidth; x<blockWidth*11-90; x+=blockWidth+2) {
				for (int y=blockHeight*3; y<blockHeight*8; y+=blockHeight+2) {
					this.blocks.add(new Block(x, y, blockWidth, blockHeight, 1));
				}
			}

			int paddleWidth = 100;
			int paddleHeight = 8;
			int ballDiameter = 10;
			this.ball = new Ball(originalScreenWidth/2 - ballDiameter/2, originalScreenHeight - paddleHeight - ballDiameter - 50, ballDiameter);
			this.paddle = new Paddle(originalScreenWidth/2 - paddleWidth/2, originalScreenHeight - paddleHeight - 50, paddleWidth, paddleHeight);

			this.timer = new java.util.Timer();
			this.timer.schedule(new RepaintTask(), 0, 1000 / this.frameRate);
		}

		class RepaintTask extends java.util.TimerTask {
			@Override
	    public void run() {
				if (view != null) {
					view.update();
				}
	    }
		}

		void addSubscriber(View v) {
			this.view = v;
		}
	}

	class View extends JPanel implements KeyListener, ComponentListener, MouseMotionListener, MouseListener {
		Model model;
		java.util.Timer fpsTimer;
		java.util.Timer rowMoveTimer;
		int framePaintCount = 0;
		int freshFrameRate = 0;
		int oldFrameRate;
		Boolean runNext = true;

		View(Model model) {
			this.model = model;
			this.addKeyListener(this);
			this.addComponentListener(this);
			this.addMouseMotionListener(this);
			this.addMouseListener(this);

			this.oldFrameRate = this.model.frameRate;
		}

		void startTimers() {
			this.fpsTimer = new java.util.Timer();
			this.fpsTimer.schedule(new RefreshFpsTask(), 0, 1000);
			startRowMoveTimer();
		}

		void startRowMoveTimer() {
			this.rowMoveTimer = new java.util.Timer();
			this.rowMoveTimer.schedule(new MoveRowDownTask(), 0, 7000);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			this.model.paddle.x = (int) (e.getX() / xScale);
		}
		@Override
		public void mouseDragged(MouseEvent e) {}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (!this.model.ball.launched) {
				this.model.ball.launch(randomWithRange(-1, 1));
			}
		}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e) {}
		@Override
		public void mouseReleased(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}

		int randomWithRange(int min, int max) {
   		int range = (max - min) + 1;
		  return (int)(Math.random() * range) + min;
		}

		class MoveRowDownTask extends java.util.TimerTask {
			@Override
	    public void run() {
				for (Block block: model.blocks) {
					block.moveDown();
				}
	    }
		}

		class RefreshFpsTask extends java.util.TimerTask {
			@Override
	    public void run() {
				freshFrameRate = framePaintCount;
				oldFrameRate = freshFrameRate;
				framePaintCount = 0;
	    }
		}

		void drawGameOverPanel(Graphics2D g2d) {
			g2d.setColor(Color.BLACK);
			g2d.fill(new Rectangle(0, 0, (int) (originalScreenWidth * xScale), (int) (originalScreenHeight * yScale) ));

			g2d.setColor(Color.GRAY);
			g2d.setFont(new Font("Comic Sans MS", Font.PLAIN, 24));
			g2d.drawString("Nice try, but game over", (int) ((originalScreenWidth/2 - 130) * xScale), (int) (originalScreenHeight/2 * yScale) );
			g2d.setColor(Color.YELLOW);
			g2d.drawString("Score: " + this.model.playerScore, (int) ((originalScreenWidth/2 - 130) * xScale), (int) ((originalScreenHeight/2 + 60) * yScale) );
		}

		void update() {
			this.model.paddle.update();
			this.model.ball.update(this.model.paddle);

			// check collisions
			Ellipse2D.Double ball = this.model.ball.getEllipse();
			for (Block block: this.model.blocks) {
				if (block.health > 0) { // alive
					int collisionAxis = block.getCollisionAxis(this.model.ball);
					if (collisionAxis != -1) {
						block.health--;
						this.model.playerScore += 10;

						if (collisionAxis == 0) { //vertical
							this.model.ball.bounceVertical();
						} else if (collisionAxis == 1) { //horizontal
							this.model.ball.bounceHorizontal();
						} else {
							System.out.println("Unhandled bounce angle - edge case");
						}
						break;
					}

					// check if block moved too low
					if (block.y + block.height > this.model.paddle.y) {
						this.model.playerLives--;

						if (this.model.playerLives == 0) {
							this.model.isGameOver = true;
							this.model.timer.cancel();
						} else {
							// reset
							this.model.ball.launched = false;
							this.model.ball.follow(this.model.paddle);

							for (Block block2: this.model.blocks) {
								block2.moveUp();
							}
						}

						break;
					}
				}
			}

			if (ball.intersects(this.model.paddle.getRectangle())) {
				this.model.playerScore += 1;
				if (this.model.paddle.moveLeft && this.model.ball.direction_x == 0) {
					this.model.ball.direction_x = -1;
					this.model.ball.bounceHorizontal();
				} else if (this.model.paddle.moveRight && this.model.ball.direction_x == 0) {
					this.model.ball.direction_x = 1;
					this.model.ball.bounceHorizontal();
				} else {
					this.model.ball.bounceHorizontal();
				}
			} else if (this.model.ball.x <= 0 || this.model.ball.x + this.model.ball.diameter >= originalScreenWidth) { // left or right
				this.model.playerScore += 1;
				this.model.ball.bounceVertical();
			} else if (this.model.ball.y <= 0) { // top
				this.model.playerScore += 1;
				this.model.ball.bounceHorizontal();
			} else if (this.model.ball.y >= originalScreenHeight) {
				this.model.playerLives--;
				if (this.model.playerLives == 0) {
					this.model.isGameOver = true;
					this.model.timer.cancel();
				} else {
					// reset
					this.model.ball.launched = false;
					this.model.ball.follow(this.model.paddle);
				}
			}

			repaint();
			this.framePaintCount++;
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setColor(Color.BLACK);
			g2d.fill(new Rectangle(0, 0, (int) (originalScreenWidth * xScale), (int) (originalScreenHeight * yScale) ));

			g2d.setColor(Color.YELLOW);
			g2d.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));
			g2d.drawString("Score: " + this.model.playerScore, 2, 18 );

			g2d.setColor(Color.RED);
			g2d.setFont(new Font("Comic Sans MS", Font.PLAIN, 14));
			g2d.drawString("Lives: " + "x x x ".substring(0, this.model.playerLives * 2), 2, 36 );

			int fps;
			if (framePaintCount != freshFrameRate) { // timer is still counting
				fps = oldFrameRate;
			} else {
				fps = freshFrameRate;
			}
			g2d.setColor(Color.GRAY);
			g2d.setFont(new Font("Comic Sans MS", Font.PLAIN, 10));
			g2d.drawString("FPS: " + fps, 2, 52 );

			for (Block block: this.model.blocks) {
				if (block.health > 0) {
					block.paint(g2d);
				}
			}

			this.model.paddle.paint(g2d);
			this.model.ball.paint(g2d);

			if (this.model.isGameOver) {
				drawGameOverPanel(g2d);
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int key = e.getKeyCode();

			if (key == KeyEvent.VK_LEFT) {
				this.model.paddle.moveLeft = true;
				this.model.ball.moveLeft = true;
			} else if (key == KeyEvent.VK_RIGHT) {
				this.model.paddle.moveRight = true;
				this.model.ball.moveRight = true;
			} else if (key == KeyEvent.VK_SPACE) {
				int paddleMovement = 0;
				if (this.model.paddle.moveLeft) {
					paddleMovement = -1;
				} else if (this.model.paddle.moveRight) {
					paddleMovement = 1;
				}
				this.model.ball.launch(paddleMovement);
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
			int key = e.getKeyCode();

			if (key == KeyEvent.VK_LEFT) {
				this.model.paddle.moveLeft = false;
				this.model.ball.moveLeft = false;
			} else if (key == KeyEvent.VK_RIGHT) {
				this.model.paddle.moveRight = false;
				this.model.ball.moveRight = false;
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void componentResized(ComponentEvent e) {
			xScale = (double) e.getComponent().getWidth() / (double) originalScreenWidth;
			yScale = (double) e.getComponent().getHeight() / (double) originalScreenHeight;

			e.getComponent().repaint();
		}

		@Override
		public void componentHidden(ComponentEvent e) {}

		@Override
		public void componentShown(ComponentEvent e) {}

		@Override
		public void componentMoved(ComponentEvent e) {}
	}

	class Splash extends JPanel implements ComponentListener {
		Splash() {
			this.addComponentListener(this);
		}

		@Override
		public void componentResized(ComponentEvent e) {
			xScale = (double) e.getComponent().getWidth() / (double) originalScreenWidth;
			yScale = (double) e.getComponent().getHeight() / (double) originalScreenHeight;

			e.getComponent().repaint();
		}

		@Override
		public void componentHidden(ComponentEvent e) {}

		@Override
		public void componentShown(ComponentEvent e) {}

		@Override
		public void componentMoved(ComponentEvent e) {}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setColor(Color.BLACK);
			g2d.fill(new Rectangle(0, 0, (int) (originalScreenWidth * xScale), (int) (originalScreenHeight * yScale) ));

			g2d.setColor(Color.GRAY);
			g2d.setFont(new Font("Comic Sans MS", Font.PLAIN, 48));
			g2d.drawString("BREAKOUT", (int) ((originalScreenWidth/2 - 310) * xScale), (int) (100 * yScale) );

			g2d.setFont(new Font("Comic Sans MS", Font.PLAIN, 24));
			g2d.setColor(Color.YELLOW);
			g2d.drawString("By Clement Hoang", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (160 * yScale) );
			g2d.drawString("User ID: c8hoang", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (210 * yScale) );
			g2d.drawString("Student #: 20531116", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (260 * yScale) );
			g2d.drawString("Instructions:", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (310 * yScale) );
			g2d.drawString("- Press the arrow keys or use the mouse to move", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (360 * yScale) );
			g2d.drawString("- Press spacebar to launch from the paddle", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (410 * yScale) );
			g2d.drawString("- Break as many bricks as you can. You have 3 lives!", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (460 * yScale) );

			g2d.setColor(Color.GREEN);
			g2d.drawString("Please click with your mouse to continue...", (int) ((originalScreenWidth/2 - 290) * xScale), (int) (510 * yScale) );
		}
	}

	public static void main(String[] args) {
		int ballSpeed = Integer.parseInt(args[0]);
		int frameRate = Integer.parseInt(args[1]);

		Breakout game = new Breakout(ballSpeed, frameRate);

		JFrame window = new JFrame("Breakout");
		window.setSize(800, 600);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(true);
		window.setLocationByPlatform(true);

		window.setContentPane(game.splashView);

		window.addMouseListener(new ClickListener(window, game.view));

		window.setVisible(true);
	}

	public static class ClickListener extends MouseAdapter {

		JFrame parent;
		View gameView;

		ClickListener(JFrame window, View gameView) {
			this.parent = window;
			this.gameView = gameView;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			this.parent.removeMouseListener(this);

			this.parent.setContentPane(gameView);
			this.parent.setVisible(true);

			gameView.setFocusable(true);
      gameView.requestFocusInWindow();
			gameView.startTimers();
		}
	}
}
