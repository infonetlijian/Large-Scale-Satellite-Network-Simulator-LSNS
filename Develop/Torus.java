package Develop;

import javax.vecmath.*;
import java.awt.*;
import java.awt.event.*;
import javax.media.j3d.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;

public class Torus extends Shape3D {
  public Torus(double r1, double r2) {
    int m = 20;
    int n = 40;
    Point3d[] pts = new Point3d[m];
    pts[0] = new Point3d(r1+r2, 0, 0);
    double theta = 2.0 * Math.PI / m;
    double c = Math.cos(theta);
    double s = Math.sin(theta);
    double[] mat = {c, -s, 0, r2*(1-c),
                    s, c, 0, -r2*s,
                    0, 0, 1, 0,
                    0, 0, 0, 1};
    Transform3D rot1 = new Transform3D(mat);
    for (int i = 1; i < m; i++) {
      pts[i] = new Point3d();
      rot1.transform(pts[i-1], pts[i]);
    }

    Transform3D rot2 = new Transform3D();
    rot2.rotY(2.0*Math.PI/n);
    IndexedQuadArray qa = new IndexedQuadArray(m*n,
      IndexedQuadArray.COORDINATES, 4*m*n);
    int quadIndex = 0;
    for (int i = 0; i < n; i++) {
      qa.setCoordinates(i*m, pts);
      for (int j = 0; j < m; j++) {
        rot2.transform(pts[j]);
        int[] quadCoords = {i*m+j, ((i+1)%n)*m+j,
          ((i+1)%n)*m+((j+1)%m), i*m+((j+1)%m)};
        qa.setCoordinateIndices(quadIndex, quadCoords);
        quadIndex += 4;
      }
    }
    GeometryInfo gi = new GeometryInfo(qa);
    NormalGenerator ng = new NormalGenerator();
    ng.generateNormals(gi);
    this.setGeometry(gi.getGeometryArray());
  }
}