package org.glob3.mobile.generated; 
//
//  Tile.cpp
//  G3MiOSSDK
//
//  Created by Agustín Trujillo Pino on 12/06/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

//
//  Tile.hpp
//  G3MiOSSDK
//
//  Created by Agustín Trujillo Pino on 12/06/12.
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//



//class G3MRenderContext;
//class Mesh;
//class TileTessellator;
//class TileTexturizer;
//class TilesRenderParameters;
//class ITimer;
//class TilesStatistics;
//class TileRenderContext;
//class TileKey;
//class Vector3D;
//class GLState;

public class Tile
{
  private TileTexturizer _texturizer;
  private Tile _parent;
  private final Sector _sector ;
  private final int _level;
  private final int _row;
  private final int _column;

  private Mesh _tessellatorMesh;
  private Mesh _debugMesh;
  private Mesh _texturizedMesh;

  private boolean _textureSolved;
  private java.util.ArrayList<Tile> _subtiles;
  private boolean _justCreatedSubtiles;

  private boolean _texturizerDirty;

  private Mesh getTessellatorMesh(G3MRenderContext rc, TileRenderContext trc)
  {
    if (_tessellatorMesh == null)
    {
      _tessellatorMesh = trc.getTessellator().createMesh(rc, this);
    }
    return _tessellatorMesh;
  }

  private Mesh getDebugMesh(G3MRenderContext rc, TileRenderContext trc)
  {
    if (_debugMesh == null)
    {
      _debugMesh = trc.getTessellator().createDebugMesh(rc, this);
    }
    return _debugMesh;
  }

  private boolean isVisible(G3MRenderContext rc, TileRenderContext trc)
  {
    // test if sector is back oriented with respect to the camera
    //  if (_sector.isBackOriented(rc)) {
    //    return false;
    //  }
  
    Extent extent = getTessellatorMesh(rc, trc).getExtent();
    if (extent == null)
    {
      return false;
    }
    return extent.touches(rc.getCurrentCamera().getFrustumInModelCoordinates());
    //return extent->touches( rc->getCurrentCamera()->getHalfFrustuminModelCoordinates() );
  }

  private boolean meetsRenderCriteria(G3MRenderContext rc, TileRenderContext trc)
  {
    final TilesRenderParameters parameters = trc.getParameters();
  
    if (_level >= parameters._maxLevel)
    {
      return true;
    }
  
    //  if (timer != NULL) {
    //    if ( timer->elapsedTime().milliseconds() > 50 ) {
    //      return true;
    //    }
    //  }
  
    TileTexturizer texturizer = trc.getTexturizer();
    if (texturizer != null)
    {
      if (texturizer.tileMeetsRenderCriteria(this))
      {
        return true;
      }
    }
  
    Extent extent = getTessellatorMesh(rc, trc).getExtent();
    if (extent == null)
    {
      return true;
    }
    //  const double projectedSize = extent->squaredProjectedArea(rc);
    //  if (projectedSize <= (parameters->_tileTextureWidth * parameters->_tileTextureHeight * 2)) {
    //    return true;
    //  }
    final Vector2I ex = extent.projectedExtent(rc);
    //const double t = extent.maxAxis() * 2;
    final int t = (ex._x + ex._y);
    if (t <= ((parameters._tileTextureWidth + parameters._tileTextureHeight) * 1.75))
    {
      return true;
    }
  
  
    if (trc.getParameters()._useTilesSplitBudget)
    {
      if (_subtiles == null) // the tile needs to create the subtiles
      {
        if (trc.getStatistics().getSplitsCountInFrame() > 1)
        {
          // there are not more splitsCount-budget to spend
          return true;
        }
  
        if (trc.getLastSplitTimer().elapsedTime().milliseconds() < 25)
        {
          // there are not more time-budget to spend
          return true;
        }
      }
    }
  
    return false;
  }

  private java.util.ArrayList<Tile> createSubTiles()
  {
    final Geodetic2D lower = _sector.lower();
    final Geodetic2D upper = _sector.upper();
  
    final Angle midLat = Angle.midAngle(lower.latitude(), upper.latitude());
    final Angle midLon = Angle.midAngle(lower.longitude(), upper.longitude());
  
    final int nextLevel = _level + 1;
  
    java.util.ArrayList<Tile> subTiles = new java.util.ArrayList<Tile>();
    subTiles.add(createSubTile(lower.latitude(), lower.longitude(), midLat, midLon, nextLevel, 2 * _row, 2 * _column));
  
    subTiles.add(createSubTile(lower.latitude(), midLon, midLat, upper.longitude(), nextLevel, 2 * _row, 2 * _column + 1));
  
    subTiles.add(createSubTile(midLat, lower.longitude(), upper.latitude(), midLon, nextLevel, 2 * _row + 1, 2 * _column));
  
    subTiles.add(createSubTile(midLat, midLon, upper.latitude(), upper.longitude(), nextLevel, 2 * _row + 1, 2 * _column + 1));
  
    return subTiles;
  }

  private void rawRender(G3MRenderContext rc, TileRenderContext trc, GLState parentState)
  {
  
    Mesh tessellatorMesh = getTessellatorMesh(rc, trc);
    if (tessellatorMesh == null)
    {
      return;
    }
  
    TileTexturizer texturizer = trc.getTexturizer();
    if (texturizer == null)
    {
      tessellatorMesh.render(rc, parentState);
    }
    else
    {
      // const bool needsToCallTexturizer = (!isTextureSolved() || (_texturizedMesh == NULL)) && isTexturizerDirty();
      final boolean needsToCallTexturizer = (_texturizedMesh == null) || isTexturizerDirty();
  
      if (needsToCallTexturizer)
      {
        _texturizedMesh = texturizer.texturize(rc, trc, this, tessellatorMesh, _texturizedMesh);
      }
  
      if (_texturizedMesh != null)
      {
        _texturizedMesh.render(rc, parentState);
      }
      else
      {
        tessellatorMesh.render(rc, parentState);
      }
    }
  
  }

  private void debugRender(G3MRenderContext rc, TileRenderContext trc, GLState parentState)
  {
    Mesh debugMesh = getDebugMesh(rc, trc);
    if (debugMesh != null)
    {
      debugMesh.render(rc, parentState);
    }
  }

  private Tile createSubTile(Angle lowerLat, Angle lowerLon, Angle upperLat, Angle upperLon, int level, int row, int column)
  {
    return new Tile(_texturizer, this, new Sector(new Geodetic2D(lowerLat, lowerLon), new Geodetic2D(upperLat, upperLon)), level, row, column);
  }


  private java.util.ArrayList<Tile> getSubTiles()
  {
    if (_subtiles == null)
    {
      _subtiles = createSubTiles();
      _justCreatedSubtiles = true;
    }
    return _subtiles;
  }

//C++ TO JAVA CONVERTER TODO TASK: The implementation of the following method could not be found:
//  Tile(Tile that);

  private void ancestorTexturedSolvedChanged(Tile ancestor, boolean textureSolved)
  {
    if (textureSolved && isTextureSolved())
    {
      return;
    }
  
    if (_texturizer != null)
    {
      _texturizer.ancestorTexturedSolvedChanged(this, ancestor, textureSolved);
    }
  
    if (_subtiles != null)
    {
      final int subtilesSize = _subtiles.size();
      for (int i = 0; i < subtilesSize; i++)
      {
        Tile subtile = _subtiles.get(i);
        subtile.ancestorTexturedSolvedChanged(ancestor, textureSolved);
      }
    }
  }

  private boolean _isVisible;
  private void setIsVisible(boolean isVisible, TileTexturizer texturizer)
  {
    if (_isVisible != isVisible)
    {
      _isVisible = isVisible;
  
      if (!_isVisible)
      {
        deleteTexturizedMesh(texturizer);
      }
    }
  }

  private void deleteTexturizedMesh(TileTexturizer texturizer)
  {
    if ((_level > 0) && (_texturizedMesh != null))
    {
  
      if (texturizer != null)
      {
        texturizer.tileMeshToBeDeleted(this, _texturizedMesh);
      }
  
      if (_texturizedMesh != null)
         _texturizedMesh.dispose();
      _texturizedMesh = null;
  
      if (_texturizerData != null)
         _texturizerData.dispose();
      _texturizerData = null;
  
      setTexturizerDirty(true);
      setTextureSolved(false);
    }
  }

  private ITexturizerData _texturizerData;

  public Tile(TileTexturizer texturizer, Tile parent, Sector sector, int level, int row, int column)
  {
     _texturizer = texturizer;
     _parent = parent;
     _sector = new Sector(sector);
     _level = level;
     _row = row;
     _column = column;
     _tessellatorMesh = null;
     _debugMesh = null;
     _texturizedMesh = null;
     _textureSolved = false;
     _texturizerDirty = true;
     _subtiles = null;
     _justCreatedSubtiles = false;
     _isVisible = false;
     _texturizerData = null;
    //  int __remove_tile_print;
    //  printf("Created tile=%s\n deltaLat=%s deltaLon=%s\n",
    //         getKey().description().c_str(),
    //         _sector.getDeltaLatitude().description().c_str(),
    //         _sector.getDeltaLongitude().description().c_str()
    //         );
  }

  public void dispose()
  {
    prune(null);
  
    if (_debugMesh != null)
       _debugMesh.dispose();
    _debugMesh = null;
    if (_tessellatorMesh != null)
       _tessellatorMesh.dispose();
    _tessellatorMesh = null;
    if (_texturizerData != null)
       _texturizerData.dispose();
    _texturizerData = null;
    if (_texturizedMesh != null)
       _texturizedMesh.dispose();
    _texturizedMesh = null;
  }


  public final Sector getSector()
  {
    return _sector;
  }

  public final int getLevel()
  {
    return _level;
  }

  public final int getRow()
  {
    return _row;
  }

  public final int getColumn()
  {
    return _column;
  }

  public final Mesh getTexturizedMesh()
  {
    return _texturizedMesh;
  }

  public final Tile getParent()
  {
    return _parent;
  }

  public final void prepareForFullRendering(G3MRenderContext rc, TileRenderContext trc)
  {
    Mesh tessellatorMesh = getTessellatorMesh(rc, trc);
    if (tessellatorMesh == null)
    {
      return;
    }
  
    TileTexturizer texturizer = trc.getTexturizer();
    if (texturizer != null)
    {
      // const bool needsToCallTexturizer = (!isTextureSolved() || (_texturizedMesh == NULL)) && isTexturizerDirty();
      final boolean needsToCallTexturizer = (_texturizedMesh == null) || isTexturizerDirty();
  
      if (needsToCallTexturizer)
      {
        _texturizedMesh = texturizer.texturize(rc, trc, this, tessellatorMesh, _texturizedMesh);
      }
    }
  }

  public final void render(G3MRenderContext rc, TileRenderContext trc, GLState parentState, java.util.LinkedList<Tile> toVisitInNextIteration)
  {
    TilesStatistics statistics = trc.getStatistics();
  
    statistics.computeTileProcessed(this);
    if (isVisible(rc, trc))
    {
      setIsVisible(true, trc.getTexturizer());
  
      statistics.computeVisibleTile(this);
  
      final boolean isRawRender = ((toVisitInNextIteration == null) || meetsRenderCriteria(rc, trc) || (trc.getParameters()._incrementalTileQuality && !_textureSolved));
  
      if (isRawRender)
      {
        rawRender(rc, trc, parentState);
        if (trc.getParameters()._renderDebug)
        {
          debugRender(rc, trc, parentState);
        }
  
        statistics.computeTileRendered(this);
  
        prune(trc.getTexturizer());
      }
      else
      {
        java.util.ArrayList<Tile> subTiles = getSubTiles();
        if (_justCreatedSubtiles)
        {
          trc.getLastSplitTimer().start();
          statistics.computeSplitInFrame();
          _justCreatedSubtiles = false;
        }
  
        final int subTilesSize = subTiles.size();
        for (int i = 0; i < subTilesSize; i++)
        {
          Tile subTile = subTiles.get(i);
          toVisitInNextIteration.addLast(subTile);
        }
      }
    }
    else
    {
      setIsVisible(false, trc.getTexturizer());
  
      prune(trc.getTexturizer());
    }
  }

  public final TileKey getKey()
  {
    return new TileKey(_level, _row, _column);
  }

  public final void setTextureSolved(boolean textureSolved)
  {
    if (textureSolved != _textureSolved)
    {
      _textureSolved = textureSolved;
  
      if (_subtiles != null)
      {
        final int subtilesSize = _subtiles.size();
        for (int i = 0; i < subtilesSize; i++)
        {
          Tile subtile = _subtiles.get(i);
          subtile.ancestorTexturedSolvedChanged(this, _textureSolved);
        }
      }
    }
  }

  public final boolean isTextureSolved()
  {
    return _textureSolved;
  }

  public final void setTexturizerDirty(boolean texturizerDirty)
  {
    _texturizerDirty = texturizerDirty;
  }

  public final boolean isTexturizerDirty()
  {
    return _texturizerDirty;
  }

  public final boolean hasTexturizerData()
  {
    return (_texturizerData != null);
  }

  public final ITexturizerData getTexturizerData()
  {
    return _texturizerData;
  }

  public final void setTexturizerData(ITexturizerData texturizerData)
  {
    if (_texturizerData != null)
       _texturizerData.dispose();
    _texturizerData = texturizerData;
  }

  public final Tile getDeepestTileContaining(Geodetic3D position)
  {
    if (_sector.contains(position))
    {
      if (_subtiles == null)
      {
        return this;
      }
  
      for (int i = 0; i < _subtiles.size(); i++)
      {
        final Tile subtile = _subtiles.get(i);
        final Tile subtileResult = subtile.getDeepestTileContaining(position);
        if (subtileResult != null)
        {
          return subtileResult;
        }
      }
    }
  
    return null;
  }

  public final void prune(TileTexturizer texturizer)
  {
    if (_subtiles != null)
    {
  
      //    printf("= pruned tile %s\n", getKey().description().c_str());
  
      //    TileTexturizer* texturizer = (trc == NULL) ? NULL : trc->getTexturizer();
  
      final int subtilesSize = _subtiles.size();
      for (int i = 0; i < subtilesSize; i++)
      {
        Tile subtile = _subtiles.get(i);
  
        subtile.setIsVisible(false, texturizer);
  
        subtile.prune(texturizer);
        if (texturizer != null)
        {
          texturizer.tileToBeDeleted(subtile, subtile._texturizedMesh);
        }
        if (subtile != null)
           subtile.dispose();
      }
  
      _subtiles = null;
      _subtiles = null;
  
    }
  }

}