import 'dart:io';

import 'package:flutter/material.dart';
import 'package:media_picker_builder/data/media_file.dart';
import 'package:media_picker_builder/media_picker_builder.dart';
import 'package:media_picker_builder_example/grid/image_grid_page.dart';
import 'package:media_picker_builder_example/picker/picker_widget.dart';
import 'package:permission_handler/permission_handler.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final navigatorKey = GlobalKey<NavigatorState>();

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: navigatorKey,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Picker Demo'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              RaisedButton(
                child: const Text("Albums"),
                onPressed: () {
                  _checkPermission().then((granted) {
                    if (!granted) return;
                    // To build your own custom picker use this api
                    MediaPickerBuilder.getAlbums(
                      withImages: true,
                      withVideos: true,
                    ).then((albums) {
                      print(albums);
                    });
                    // If you are happy with the example picker then you use this!
                    _buildPicker();
                  });
                },
              ),
              SizedBox(
                height: 10.0,
              ),
              RaisedButton(
                child: const Text("Videos and live photos"),
                onPressed: () {
                  _checkPermission().then((granted) {
                    if (!granted) return;
                    MediaPickerBuilder.getMediaAssets(
                      start: DateTime(2021, 1, 22),
                      end: DateTime(2021, 1, 30),
                    ).then((albums) {
                      print(albums);
                    });
                    // If you are happy with the example picker then you use this!
                    _buildPicker();
                  });
                },
              ),
              RaisedButton(
                child: Text('Date Range'),
                onPressed: () {
                  _showDateRangePicker();
                },
              )
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _showDateRangePicker() async {
    final granted = await _checkPermission();

    if (!granted) return;

    final range = await showDateRangePicker(
      context: navigatorKey.currentState.overlay.context,
      firstDate: DateTime(2000),
      lastDate: DateTime.now(),
      initialDateRange: DateTimeRange(
        start: DateTime.now().subtract(Duration(days: 7)),
        end: DateTime.now(),
      ),
    );

    final route = MaterialPageRoute(
      builder: (context) => ImageGridPage(range: range),
    );

    Navigator.push(navigatorKey.currentContext, route);
  }

  _buildPicker() {
    showModalBottomSheet<Set<MediaFile>>(
      context: navigatorKey.currentState.overlay.context,
      builder: (BuildContext context) {
        return PickerWidget(
          withImages: true,
          withVideos: true,
          onDone: (Set<MediaFile> selectedFiles) {
            print(selectedFiles);
            Navigator.pop(context);
          },
          onCancel: () {
            print("Cancelled");
            Navigator.pop(context);
          },
        );
      },
    );
  }

  Future<bool> _checkPermission() async {
    final permission = Platform.isIOS ? Permission.photos : Permission.storage;

    var status = await permission.status;

    if (status.isUndetermined) {
      status = await permission.request();
    }

    return status == PermissionStatus.granted;
  }
}
