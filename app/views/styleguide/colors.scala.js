@()(implicit r: RequestHeader)

@shared.requireJsConfig()
@shared.jsRoutes()

define("config/styleguide/colors", function() {
  return {
    containerId: "colorContainer"
  };
});
