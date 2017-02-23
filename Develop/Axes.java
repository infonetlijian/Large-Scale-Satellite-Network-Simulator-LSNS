package Develop;

import javax.vecmath.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;

public class Axes extends Group {
  public Axes() {
    Appearance ap = new Appearance();
    ap.setMaterial(new Material());
    Font3D font = new Font3D(new Font("SanSerif", Font.PLAIN, 1),
                             new FontExtrusion());
    Text3D x = new Text3D(font, "x");
    Shape3D xShape = new Shape3D(x, ap);
    Text3D y = new Text3D(font, "y");
    Shape3D yShape = new Shape3D(y, ap);
    Text3D z = new Text3D(font, "z");
    Shape3D zShape = new Shape3D(z, ap);
    // transform for texts
    Transform3D tTr = new Transform3D();
    tTr.setTranslation(new Vector3d(-0.12, 0.6, -0.04));
    tTr.setScale(0.5);
    // transform for arrows
    Transform3D aTr = new Transform3D();
    aTr.setTranslation(new Vector3d(0, 0.5, 0));
    // x axis
    Cylinder xAxis = new Cylinder(0.05f, 1f);
    Transform3D xTr = new Transform3D();
    xTr.setRotation(new AxisAngle4d(0, 0, 1, -Math.PI/2));
    xTr.setTranslation(new Vector3d(0.5, 0, 0));
    TransformGroup xTg = new TransformGroup(xTr);
    xTg.addChild(xAxis);
    this.addChild(xTg);
    TransformGroup xTextTg = new TransformGroup(tTr);
    xTextTg.addChild(xShape);
    xTg.addChild(xTextTg);
    Cone xArrow = new Cone(0.1f, 0.2f);
    TransformGroup xArrowTg = new TransformGroup(aTr);
    xArrowTg.addChild(xArrow);
    xTg.addChild(xArrowTg);
    // y axis
    Cylinder yAxis = new Cylinder(0.05f, 1f);
    Transform3D yTr = new Transform3D();
    yTr.setTranslation(new Vector3d(0, 0.5, 0));
    TransformGroup yTg = new TransformGroup(yTr);
    yTg.addChild(yAxis);
    this.addChild(yTg);
    TransformGroup yTextTg = new TransformGroup(tTr);
    yTextTg.addChild(yShape);
    yTg.addChild(yTextTg);
    Cone yArrow = new Cone(0.1f, 0.2f);
    TransformGroup yArrowTg = new TransformGroup(aTr);
    yArrowTg.addChild(yArrow);
    yTg.addChild(yArrowTg);
    // z axis
    Cylinder zAxis = new Cylinder(0.05f, 1f);
    Transform3D zTr = new Transform3D();
    zTr.setRotation(new AxisAngle4d(1, 0, 0, Math.PI/2));
    zTr.setTranslation(new Vector3d(0, 0, 0.5));
    TransformGroup zTg = new TransformGroup(zTr);
    zTg.addChild(zAxis);
    this.addChild(zTg);
    TransformGroup zTextTg = new TransformGroup(tTr);
    zTextTg.addChild(zShape);
    zTg.addChild(zTextTg);
    Cone zArrow = new Cone(0.1f, 0.2f);
    TransformGroup zArrowTg = new TransformGroup(aTr);
    zArrowTg.addChild(zArrow);
    zTg.addChild(zArrowTg);
  }
}
