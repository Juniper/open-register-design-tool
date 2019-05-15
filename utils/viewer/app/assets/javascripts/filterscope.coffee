# filterscope.coffee - calls getAncestors with input id and uses returned json
#                      to generate filter scope path
#
$ ->
   idElems = $("#filterscope")
   id = idElems.first().data('cid')
#   alert("id = " + id)
   rr = jsRoutes.controllers.Application.getAncestors(id)
   #alert("type=" + rr.type + ", url=" + rr.url)
   $.get rr.url, (registers) ->
 # create ancestor path
      path = '' 
      $.each registers, (index, register) ->
         if index > 0 then prefix = '.' else prefix = ''
         rr = jsRoutes.controllers.Application.filterPage(register.id)
         #alert("type=" + rr.type + ", url=" + rr.url)
         path += prefix + '<a href="' + rr.url + '">' + register.name + '</a.>' 
      pathinfo = $("#{path}")
      idElems.append pathinfo     
