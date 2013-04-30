//
//  WMSBillElevationDataProvider.cpp
//  G3MiOSSDK
//
//  Created by Diego Gomez Deck on 2/17/13.
//
//

#include "WMSBillElevationDataProvider.hpp"

#include "Context.hpp"
#include "IDownloader.hpp"
#include "IStringBuilder.hpp"
#include "Sector.hpp"
#include "Vector2I.hpp"
#include "URL.hpp"
#include "TimeInterval.hpp"
#include "IBufferDownloadListener.hpp"
#include "BilParser.hpp"
#include "ShortBufferElevationData.hpp"


class WMSBillElevationDataProvider_BufferDownloadListener : public IBufferDownloadListener {
private:
  const Sector            _sector;
  const int               _width;
  const int               _height;
  const double            _noDataValue;

  IElevationDataListener* _listener;
  const bool              _autodeleteListener;


public:

  WMSBillElevationDataProvider_BufferDownloadListener(const Sector& sector,
                                                      const Vector2I& extent,
                                                      double noDataValue,
                                                      IElevationDataListener* listener,
                                                      bool autodeleteListener) :
  _sector(sector),
  _width(extent._x),
  _height(extent._y),
  _noDataValue(noDataValue),
  _listener(listener),
  _autodeleteListener(autodeleteListener)
  {

  }

  void onDownload(const URL& url,
                  IByteBuffer* buffer,
                  bool expired) {
    const Vector2I resolution(_width, _height);
    ShortBufferElevationData* elevationData = BilParser::parseBil16(_sector, resolution, _noDataValue, buffer);
    delete buffer;

    if (elevationData == NULL) {
      _listener->onError(_sector, resolution);
    }
    else {
      _listener->onData(_sector, resolution, elevationData);
    }

    if (_autodeleteListener) {
      delete _listener;
      _listener = NULL;
    }
  }

  void onError(const URL& url) {
    const Vector2I resolution(_width, _height);

    _listener->onError(_sector, resolution);
    if (_autodeleteListener) {
      delete _listener;
      _listener = NULL;
    }
  }

  void onCancel(const URL& url) {

  }

  void onCanceledDownload(const URL& url,
                          IByteBuffer* data,
                          bool expired) {

  }


};

void WMSBillElevationDataProvider::initialize(const G3MContext* context) {
  _downloader = context->getDownloader();
}

const long long WMSBillElevationDataProvider::requestElevationData(const Sector& sector,
                                                                   const Vector2I& extent,
                                                                   IElevationDataListener* listener,
                                                                   bool autodeleteListener) {
  if (_downloader == NULL) {
    ILogger::instance()->logError("WMSBillElevationDataProvider was not initialized.");
    return -1;
  }

  IStringBuilder *isb = IStringBuilder::newStringBuilder();

  /*
   // http://data.worldwind.arc.nasa.gov/elev?REQUEST=GetMap&SERVICE=WMS&VERSION=1.3.0&LAYERS=srtm30&STYLES=&FORMAT=image/bil&CRS=EPSG:4326&BBOX=-180.0,-90.0,180.0,90.0&WIDTH=10&HEIGHT=10


   isb->addString("http://data.worldwind.arc.nasa.gov/elev?");
   isb->addString("REQUEST=GetMap");
   isb->addString("&SERVICE=WMS");
   isb->addString("&VERSION=1.3.0");
   isb->addString("&LAYERS=srtm30");
   isb->addString("&STYLES=");
   isb->addString("&FORMAT=image/bil");
   isb->addString("&CRS=EPSG:4326");
   */

  isb->addString(_url.getPath());

  isb->addString("&BBOX=");
  isb->addDouble(sector.lower().latitude()._degrees);
  isb->addString(",");
  isb->addDouble(sector.lower().longitude()._degrees);
  isb->addString(",");
  isb->addDouble(sector.upper().latitude()._degrees);
  isb->addString(",");
  isb->addDouble(sector.upper().longitude()._degrees);

  isb->addString("&WIDTH=");
  isb->addInt(extent._x);
  isb->addString("&HEIGHT=");
  isb->addInt(extent._y);

  const std::string path = isb->getString();
  delete isb;


  const double noDataValue = 0;

  return _downloader->requestBuffer(URL(path, false),
                                    2000000000,
                                    TimeInterval::fromDays(30),
                                    true,
                                    new WMSBillElevationDataProvider_BufferDownloadListener(sector,
                                                                                            extent,
                                                                                            noDataValue,
                                                                                            listener,
                                                                                            autodeleteListener),
                                    true);
}

void WMSBillElevationDataProvider::cancelRequest(const long long requestId) {
  _downloader->cancelRequest(requestId);
}

//ElevationData* WMSBillElevationDataProvider::createSubviewOfElevationData(ElevationData* elevationData,
//                                                                          const Sector& sector,
//                                                                          const Vector2I& extent) const{
//  return new SubviewElevationData(elevationData,
//                                  false,
//                                  sector,
//                                  extent,
//                                  false);
//}
