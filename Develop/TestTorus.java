package Develop;

import javax.vecmath.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import java.applet.*;
import com.sun.j3d.utils.applet.MainFrame;

public class TestTorus extends Applet {
  public static void main(String[] args) {
    new MainFrame(new TestTorus(), 800, 480);
  }

  public void init() {
    // create canvas
    GraphicsConfiguration gc = SimpleUniverse.getPreferredConfiguration();
    Canvas3D cv = new Canvas3D(gc);
    setLayout(new BorderLayout());
    add(cv, BorderLayout.CENTER);
    BranchGroup bg = createSceneGraph();
    bg.compile();
    SimpleUniverse su = new SimpleUniverse(cv);
    su.getViewingPlatform().setNominalViewingTransform();
    su.addBranchGraph(bg);
  }

  private BranchGroup createSceneGraph() {
    BranchGroup root = new BranchGroup();
    TransformGroup spin = new TransformGroup();
    spin.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    root.addChild(spin);

    Transform3D tr = new Transform3D();						//Transform3D()表示一个4X4的双精度浮点数矩阵
    tr.setScale(0.8);
    tr.setRotation(new AxisAngle4d(1, 0, 0, Math.PI/4));
    TransformGroup tg = new TransformGroup(tr);
    spin.addChild(tg);
    
    //object
    Shape3D torus1 = new Torus(0.005, 0.5);
    Appearance ap = new Appearance();
    ap.setMaterial(new Material());
    torus1.setAppearance(ap);
    tg.addChild(torus1);
    
    Shape3D torus2 = new Torus(0.01, 0.5);
    ap = new Appearance();
    ap.setMaterial(new Material());
    
    ap.setTransparencyAttributes(
      new TransparencyAttributes(TransparencyAttributes.BLENDED, 0.5f));
    torus2.setAppearance(ap);
    Transform3D tr2 = new Transform3D();
    tr2.setRotation(new AxisAngle4d(1, 0, 0, Math.PI/2));		//用于旋转
    tr2.setTranslation(new Vector3d(1,0,0));					//setTranslation设置平移变换矩阵
    TransformGroup tg2 = new TransformGroup(tr2);
    tg.addChild(tg2);
    tg2.addChild(torus2);

    Alpha alpha = new Alpha(-1, 8000);
    RotationInterpolator rotator = new RotationInterpolator(alpha, spin);
    BoundingSphere bounds = new BoundingSphere();
    rotator.setSchedulingBounds(bounds);
    //spin.addChild(rotator);

    //background and lights
    Background background = new Background(1.0f, 1.0f, 1.0f);
    background.setApplicationBounds(bounds);
    root.addChild(background);
    AmbientLight light = new AmbientLight(true, new Color3f(Color.blue));
    light.setInfluencingBounds(bounds);
    root.addChild(light);
    PointLight ptlight = new PointLight(new Color3f(Color.white),
      new Point3f(3f,3f,3f), new Point3f(1f,0f,0f));
    ptlight.setInfluencingBounds(bounds);
    root.addChild(ptlight);
    return root;
  }
}