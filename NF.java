import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class NF extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NF f = new NF();
            f.setVisible(true);
        });
    }

    NF() {
        super("NF Shooter");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        G p = new G(800, 600);
        setContentPane(p);
        pack();
        setLocationRelativeTo(null);
        p.requestFocusInWindow();
    }

    static class G extends JPanel implements ActionListener, KeyListener {
        enum S {
            MENU, RUNNING, OVER
        }

        int W, H;
        javax.swing.Timer t;
        long last;
        S s = S.MENU;
        P pl;
        java.util.List<B> bs = new ArrayList<>();
        java.util.List<E> es = new ArrayList<>();
        int score = 0, lives = 3;
        double diff = 1, spawn = 0, spawnIv = 10;
        boolean up, down, left, right, shoot;

        G(int w, int h) {
            W = w;
            H = h;
            setPreferredSize(new Dimension(W, H));
            setFocusable(true);
            setBackground(Color.BLACK);
            addKeyListener(this);
            init();
            t = new javax.swing.Timer(16, this);
            t.start();
            last = System.nanoTime();
        }

        void init() {
            bs.clear();
            es.clear();
            score = 0;
            lives = 3;
            diff = 1;
            spawn = 0;
            pl = new P(80, H / 2.0, 40, 26);
        }

        public void actionPerformed(ActionEvent e) {
            long now = System.nanoTime();
            double dt = (now - last) / 1_000_000_000.0;
            last = now;
            if (s == S.RUNNING) {
                double vx = (right ? 1 : 0) - (left ? 1 : 0), vy = (down ? 1 : 0) - (up ? 1 : 0);
                pl.u(dt, vx, vy, shoot, bs);
                pl.x = clamp(pl.x, 0, W - pl.w);
                pl.y = clamp(pl.y, 0, H - pl.h);
                spawnIv = Math.max(0.35, 0.8 - diff * 0.06);
                diff += 0.02 * dt;
                spawn -= dt;
                if (spawn <= 0) {
                    spawn = spawnIv;
                    es.add(E.spawn(W, H, diff));
                }
                for (int i = bs.size() - 1; i >= 0; i--) {
                    B b = bs.get(i);
                    b.u(dt);
                    if (b.x > W + 40)
                        bs.remove(i);
                }
                for (int i = es.size() - 1; i >= 0; i--) {
                    E en = es.get(i);
                    en.u(dt);
                    if (en.x + en.w < -60)
                        es.remove(i);
                }
                for (int i = es.size() - 1; i >= 0; i--) {
                    E en = es.get(i);
                    Rectangle er = en.r();
                    for (int j = bs.size() - 1; j >= 0; j--) {
                        B b = bs.get(j);
                        if (er.intersects(b.r())) {
                            bs.remove(j);
                            en.hp--;
                            if (en.hp <= 0) {
                                score += en.sc;
                                es.remove(i);
                                break;
                            }
                        }
                    }
                }
                for (int i = es.size() - 1; i >= 0; i--) {
                    if (pl.r().intersects(es.get(i).r())) {
                        es.remove(i);
                        lives--;
                        pl.inv = 1.2;
                        if (lives <= 0) {
                            s = S.OVER;
                            break;
                        }
                    }
                }
            }
            repaint();
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (s == S.MENU) {
                drawC(g2, "NF SHOOTER", H / 2 - 40, Color.WHITE, 36);
                drawC(g2, "Press ENTER to start", H / 2 + 10, new Color(220, 220, 220), 18);
            }
            if (s == S.RUNNING) {
                pl.d(g2);
                for (B b : bs)
                    b.d(g2);
                for (E e : es)
                    e.d(g2);
                drawHUD(g2);
            }
            if (s == S.OVER) {
                drawC(g2, "GAME OVER", H / 2 - 40, Color.WHITE, 36);
                drawC(g2, "Score: " + score, H / 2 + 5, new Color(220, 220, 220), 18);
                drawC(g2, "Press R to retry", H / 2 + 30, new Color(200, 200, 200), 16);
            }
            g2.dispose();
        }

        void drawHUD(Graphics2D g2) {
            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(16f));
            g2.drawString("Score: " + score, 12, 22);
            g2.drawString("Lives: " + lives, 12, 42);
        }

        void drawC(Graphics2D g2, String txt, int y, Color c, int sz) {
            Font f = g2.getFont().deriveFont(Font.BOLD, (float) sz);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(txt);
            g2.setColor(c);
            g2.drawString(txt, (W - w) / 2, y);
        }

        static double clamp(double v, double lo, double hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W)
                up = true;
            else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S)
                down = true;
            else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A)
                left = true;
            else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)
                right = true;
            else if (k == KeyEvent.VK_SPACE)
                shoot = true;
            else if (k == KeyEvent.VK_ENTER && s == S.MENU)
                s = S.RUNNING;
            else if (k == KeyEvent.VK_R && s == S.OVER) {
                init();
                s = S.RUNNING;
            }
        }

        public void keyReleased(KeyEvent e) {
            int k = e.getKeyCode();
            if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W)
                up = false;
            else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S)
                down = false;
            else if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A)
                left = false;
            else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D)
                right = false;
            else if (k == KeyEvent.VK_SPACE)
                shoot = false;
        }

        public void keyTyped(KeyEvent e) {
        }

        static class Ent {
            double x, y, w, h;

            Ent(double x, double y, double w, double h) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
            }

            Rectangle r() {
                return new Rectangle((int) x, (int) y, (int) w, (int) h);
            }

            double cx() {
                return x + w / 2;
            }

            double cy() {
                return y + h / 2;
            }

            void d(Graphics2D g2) {
            }
        }

        static class P extends Ent {
            double sp = 260, cd = 0, inv = 0;

            P(double x, double y, double w, double h) {
                super(x, y, w, h);
            }

            void u(double dt, double vx, double vy, boolean shoot, java.util.List<B> out) {
                x += vx * sp * dt;
                y += vy * sp * dt;
                if (cd > 0)
                    cd -= dt;
                if (inv > 0)
                    inv -= dt;
                if (shoot && cd <= 0) {
                    cd = 0.16;
                    out.add(new B(x + w - 6, y + 6, 360));
                    out.add(new B(x + w - 6, y + h - 12, 360));
                }
            }

            void d(Graphics2D g2) {
                if (inv > 0 && ((int) (inv * 20) % 2 == 0))
                    return;
                g2.setColor(new Color(255, 174, 88));
                g2.fillRoundRect((int) x, (int) y, (int) w, (int) h, 16, 16);
                g2.setColor(new Color(120, 190, 255));
                g2.fillRoundRect((int) (x + 8), (int) (y + 6), 16, 10, 6, 6);
            }

            Rectangle r() {
                return new Rectangle((int) x + 2, (int) y + 2, (int) w - 4, (int) h - 4);
            }
        }

        static class B extends Ent {
            double vx;

            B(double x, double y, double vx) {
                super(x, y, 10, 4);
                this.vx = vx;
            }

            void u(double dt) {
                x += vx * dt;
            }

            void d(Graphics2D g2) {
                g2.setColor(new Color(255, 240, 140));
                g2.fillRoundRect((int) x, (int) y, (int) w, (int) h, 6, 6);
            }
        }

        static class E extends Ent {
            double vx, vy;
            int hp = 2, sc = 20;
            double wob = Math.random() * Math.PI * 2;
            Color body = new Color(134, 204, 220), trim = new Color(82, 160, 180);

            E(double x, double y, double w, double h, double vx, double vy, int hp, int sc, Color b, Color t) {
                super(x, y, w, h);
                this.vx = vx;
                this.vy = vy;
                this.hp = hp;
                this.sc = sc;
                body = b;
                trim = t;
            }

            static E spawn(int W, int H, double d) {
                Random r = new Random();
                int type = r.nextInt(3);
                double y = 30 + r.nextInt(H - 60);
                if (type == 0) {
                    double w = 28, h = 18, vx = -(120 + r.nextInt(120) + d * 20);
                    return new E(W + 20, y, w, h, vx, (r.nextDouble() - 0.5) * 30, 1, 15, new Color(170, 240, 140),
                            new Color(110, 190, 100));
                } else if (type == 1) {
                    double w = 38, h = 24, vx = -(80 + r.nextInt(90) + d * 12);
                    E e = new E(W + 20, y, w, h, vx, 0, 2, 20, new Color(134, 204, 220), new Color(82, 160, 180));
                    e.wob = r.nextDouble() * Math.PI * 2;
                    return e;
                } else {
                    double w = 52, h = 30, vx = -(60 + r.nextInt(60) + d * 8);
                    return new E(W + 20, y, w, h, vx, (r.nextDouble() - 0.5) * 20, 4, 40, new Color(240, 170, 160),
                            new Color(200, 120, 110));
                }
            }

            void u(double dt) {
                wob += dt * 2.2;
                y += Math.sin(wob) * 20 * dt + vy * dt;
                x += vx * dt;
            }

            void d(Graphics2D g2) {
                g2.setColor(body);
                g2.fillRoundRect((int) x, (int) y, (int) w, (int) h, 12, 12);
                g2.setColor(trim);
                g2.fillOval((int) (x + 4), (int) (y + 4), (int) (w * 0.35), (int) (h * 0.6));
                g2.setColor(Color.BLACK);
                g2.drawRoundRect((int) x, (int) y, (int) w, (int) h, 12, 12);
            }
        }
    }
}
