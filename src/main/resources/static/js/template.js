(function($) {
  'use strict';
  $(document).ready(function() {
    var body = $('body');
    var contentWrapper = $('.content-wrapper');
    var scroller = $('.container-scroller');
    var footer = $('.footer');
    var sidebar = $('.sidebar');
    var navbar = $('.navbar').not('.top-navbar');


    //Add active class to nav-link based on url dynamically
    //Active class can be hard coded directly in html file also as required

//    function addActiveClass(element) {
//      var href = element.attr('href');
//        if (!href) {
//          return; // href가 없으면 아무 작업도 하지 않음
//        }
//      if (current === "") {
//        //for root url
//        if (element.attr('href').indexOf("index.html") !== -1) {
//          element.parents('.nav-item').last().addClass('active');
//          if (element.parents('.sub-menu').length) {
//            element.closest('.collapse').addClass('show');
//            element.addClass('active');
//          }
//        }
//      } else {
//        //for other url
//        if (element.attr('href').indexOf(current) !== -1) {
//          element.parents('.nav-item').last().addClass('active');
//          if (element.parents('.sub-menu').length) {
//            element.closest('.collapse').addClass('show');
//            element.addClass('active');
//          }
//          if (element.parents('.submenu-item').length) {
//            element.addClass('active');
//          }
//        }
//      }
//    }

    function addActiveClass(element) {
      var href = element.attr('href');
      if (!href) {
        return; // href가 없으면 아무 작업도 하지 않음
      }

      // 키워드 목록 정의//RYAN 페이지 추가시 url 추가 필요 li hover기능
      var keywords = ["notice", "logboard", "calendar", "issue", "chat", "management"];

      // 루트 URL인 경우 처리: location.pathname이 "/" 또는 빈 문자열일 때
      if (location.pathname === "/" || location.pathname === "") {
        if (href.indexOf("index.html") !== -1) {
          element.parents('.nav-item').last().addClass('active');
          if (element.parents('.sub-menu').length) {
            element.closest('.collapse').addClass('show');
            element.addClass('active');
          }
        }
      } else {
        // 현재 URL(예: location.pathname)에서 키워드 중 하나라도 포함되어 있고,
        // 해당 링크의 href에도 동일한 키워드가 포함되어 있다면 active 처리
        var isKeywordMatch = keywords.some(function(keyword) {
          return location.pathname.indexOf(keyword) !== -1 && href.indexOf(keyword) !== -1;
        });
        if (isKeywordMatch) {
          element.parents('.nav-item').last().addClass('active');
          if (element.parents('.sub-menu').length) {
            element.closest('.collapse').addClass('show');
            element.addClass('active');
          }
          if (element.parents('.submenu-item').length) {
            element.addClass('active');
          }
        }
      }
    }


    var current = location.pathname.split("/").slice(-1)[0].replace(/^\/|\/$/g, '');
    $('.nav li a', sidebar).each(function() {
      var $this = $(this);
      addActiveClass($this);
    })

    //Close other submenu in sidebar on opening any

    sidebar.on('show.bs.collapse', '.collapse', function() {
      sidebar.find('.collapse.show').collapse('hide');
    });


    //Change sidebar and content-wrapper height
    applyStyles();

    function applyStyles() {
      //Applying perfect scrollbar
    }

    $('[data-toggle="minimize"]').on("click", function() {
      if (body.hasClass('sidebar-toggle-display')) {
        body.toggleClass('sidebar-hidden');
      } else {
        body.toggleClass('sidebar-icon-only');
      }
    });

    //checkbox and radios
    $(".form-check label,.form-radio label").append('<i class="input-helper"></i>');


    // fixed navbar on scroll
    $(window).scroll(function() {
      if(window.matchMedia('(min-width: 991px)').matches) {
        if ($(window).scrollTop() >= 197) {
          $(navbar).addClass('navbar-mini fixed-top');
          $(body).addClass('navbar-fixed-top');
        } else {
          $(navbar).removeClass('navbar-mini fixed-top');
          $(body).removeClass('navbar-fixed-top');
        }
      }
      if(window.matchMedia('(max-width: 991px)').matches) {
        $(navbar).addClass('navbar-mini fixed-top');
        $(body).addClass('navbar-fixed-top');
      } 
    });  
    var proBanner = document.querySelector('#proBanner');
    if (proBanner) {
      if ($.cookie('spica-free-banner') !== "true") {
        proBanner.classList.add('d-flex');
      } else {
        proBanner.classList.add('d-none');
      }

      var bannerClose = document.querySelector('#bannerClose');
      if (bannerClose) {
        bannerClose.addEventListener('click', function() {
          proBanner.classList.add('d-none');
          proBanner.classList.remove('d-flex');
          var date = new Date();
          date.setTime(date.getTime() + 24 * 60 * 60 * 1000);
          $.cookie('spica-free-banner', "true", { expires: date });
        });
      }
    }
  });
})(jQuery);