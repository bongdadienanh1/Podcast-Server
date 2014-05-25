// Include gulp and Our Plugins
var gulp = require('gulp'), 
    jshint = require('gulp-rename'), 
    less = require('gulp-less'), 
    minifyCSS = require('gulp-minify-css'),
    ngmin = require('gulp-ngmin'),
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    ngHtml2Js = require("gulp-ng-html2js"),
    addsrc = require('gulp-add-src');

    // Location Files :
var jsLocation = ['src/main/webapp/app/js/*.js', '!src/main/webapp/app/js/all*.js', '!src/main/webapp/app/js/*.min.js']
    lessLocation = 'src/main/webapp/app/less/*.less',
    htmlLocation = 'src/main/webapp/app/html/*.html'

var cssDestionation = 'src/main/webapp/app/css'
    jsDestination = 'src/main/webapp/app/js';

// Lint Task
gulp.task('lint', function() {
    return gulp.src(jsLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});

gulp.task('scripts', function() {
    gulp.src(htmlLocation)
    .pipe(ngHtml2Js({
        moduleName: "PodcastAppPartial",
        prefix: "/html/"
    }))
    .pipe(concat("partials.js"))
    //.pipe(gulp.dest(jsDestination))
    .pipe(addsrc(jsLocation))
    .pipe(concat('all.js'))
    .pipe(gulp.dest(jsDestination))
    .pipe(ngmin())
    .pipe(rename('all.min.js'))
    .pipe(uglify())
    .pipe(gulp.dest(jsDestination));
});


gulp.task('less', function () {
    gulp.src(lessLocation)
        .pipe(less())
        .pipe(minifyCSS({keepBreaks: true}))
        .pipe(gulp.dest(cssDestionation));
});

// Watch Files For Changes
gulp.task('watch', function() {
    gulp.watch(jsLocation, ['lint']);
    gulp.watch(jsLocation, ['scripts']);
    gulp.watch(lessLocation, ['less']);
});

// Default Task
gulp.task('default', ['lint', 'less', 'watch', 'scripts']);