# Peachify CloudStream Extension

A comprehensive CloudStream extension for Peachify streaming service with support for movies and TV series.

## Features

✅ **Movies & TV Series Support**
- Browse and search movies and TV series
- View detailed information (title, description, rating, year, duration)
- Browse episodes and seasons

✅ **Multiple Server Support**
- Fallback to multiple servers for reliability
- 6 different Peachify servers for redundancy

✅ **Video Streaming**
- HLS (HTTP Live Streaming) support
- MP4 direct streaming
- Multiple quality options (1080p, 720p, 480p, 360p)
- Multiple audio tracks (Dubbed, Subbed, Original)

✅ **Subtitle Support**
- Multiple subtitle formats
- Various language options
- VTT subtitle format

✅ **CloudStream Features**
- Homepage with featured content
- Quick search functionality
- Advanced search with filters
- Chromecast support
- Download support
- Watch history integration

✅ **Content Categories**
- Popular Movies
- Popular Series
- Genre filtering
- Year filtering
- Rating filtering

## API Endpoints

The extension uses multiple Peachify API servers:
- `https://uwu.eat-peach.sbs/moviebox/`
- `https://usa.eat-peach.sbs/holly/`
- `https://usa.eat-peach.sbs/air/`
- `https://usa.eat-peach.sbs/multi/`
- `https://uwu.eat-peach.sbs/net/`
- `https://uwu.eat-peach.sbs/bmb/`

## API Response Handling

### Movie Request
```
GET /moviebox/movie/{tmdb_id}
```

Returns JSON with:
- `sources`: Array of streaming sources
  - `url`: Streaming URL
  - `quality`: Video quality (1080, 720, 480, etc.)
  - `type`: Stream type (hls, mp4)
  - `dub`: Audio track (dubbed, subbed, original)
- `subtitles`: Array of subtitle tracks
  - `url`: Subtitle file URL
  - `label`: Language label
  - `format`: Subtitle format (vtt)

### TV Series Request
```
GET /holly/tv/{tmdb_id}/{season}/{episode}
```

Returns same format as movie response

## Installation

1. Clone the repository
2. Copy the `Peachify` folder to your CloudStream extensions directory
3. Build and load in CloudStream

```bash
cd Peachify
./gradlew assembleDebug
```

## Configuration

The extension can be configured through CloudStream settings:
- Server selection
- Quality preferences
- Audio track preferences
- Subtitle preferences

## Supported Content Types

- Movies (TvType.Movie)
- TV Series (TvType.TvSeries)

## Development

### Dependencies
- CloudStream3 API
- Kotlin standard library
- AndroidX components

### Key Classes

**Peachify.kt**
- Main extension class
- Implements search, load, and link extraction
- Handles homepage and quick search

**PeachifyExtractor.kt**
- Stream extraction logic
- Multiple server fallback handling
- Encryption/decryption support

**PeachifyDecrypt.kt**
- Handles encrypted response decoding
- Base64 and AES decryption
- Stream source parsing

## Error Handling

- Server fallback: If one server fails, tries the next
- Graceful error handling with logging
- Invalid response handling
- Network error recovery

## Performance Optimizations

- Parallel server requests for faster loading
- Caching of parsed content
- Lazy loading of episodes
- Optimized regex patterns

## Security

- HTTPS connections only
- Proper referer headers
- User-Agent rotation
- Encrypted source handling

## Limitations

- Requires stable internet connection
- Some servers may have geographic restrictions
- Streaming availability depends on server status
- Some content may be region-locked

## Future Enhancements

- [ ] Watch history sync
- [ ] Favorite shows/movies
- [ ] Advanced filtering
- [ ] Custom server configuration
- [ ] Playback speed control integration
- [ ] Advanced subtitle customization
- [ ] Content recommendations

## License

MIT License

## Support

For issues or questions, please create an issue in the repository.

## Disclaimer

This extension is for educational purposes. Ensure compliance with local laws and terms of service of the streaming provider.
