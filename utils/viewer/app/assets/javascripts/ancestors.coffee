# ancestors.coffee - calls getAncestors with input id and uses returned json
#                      to generate description header
#
$ ->
   # ----- functions

   # create a category string
   createCatString = (cat) ->
     result = ''
     if cat & 1 then result += ' STATIC_CONFIG' 
     if cat & 2 then result += ' DYNAMIC_CONFIG' 
     if cat & 4 then result += ' CONSTRAINED_CONFIG' 
     if cat & 8 then result += ' STAT_COUNTER' 
     if cat & 16 then result += ' ERROR_COUNTER' 
     if cat & 32 then result += ' STATE' 
     if cat & 64 then result += ' INTERRUPT' 
     if cat & 128 then result += ' DIAGNOSTIC' 
     if cat & 256 then result += ' DEBUG' 
     if cat & 512 then result += ' CGATE_UNSAFE' 
     result
   
   # ----- create page header with ancestor info
   ancIdElems = $("#ancestors")
   id = ancIdElems.first().data('id')
   mdepth = ancIdElems.first().data('mdepth')
#   alert("id = " + id)
   rr = jsRoutes.controllers.Application.getAncestors(id)
   #alert("type=" + rr.type + ", url=" + rr.url)
   $.get rr.url, (registers) ->
      #
      # ---- create clickable ancestor path
      path = '' 
      $.each registers, (index, register) ->
         if index > 0 then prefix = '.' else prefix = ''
         if register.isRegset
            rr = jsRoutes.controllers.Application.regsetPage(register.id, register.depth, mdepth)
         else
            rr = jsRoutes.controllers.Application.registerPage(register.id)
         #alert("type=" + rr.type + ", url=" + rr.url)
         path += prefix + '<a href="' + rr.url + '">' + register.name + '</a>' 
      hdr = $("<h3>#{path}</h3>")
      ancIdElems.append hdr
      #
      # ---- display reg/regset description and address info
      if registers.length
#         alert "current:"
         current = registers[registers.length-1]
#         alert "current id is #{current.id}"
         if current.shortText?
            txt = $("<h4>#{current.shortText}</h4>")
            ancIdElems.append txt   
         txt = $("<h5><b>base address: </b>0x#{current.baseAddr.toString(16)}</h5>")
         ancIdElems.append txt
         if current.highAddr?
            txt = $("<h5><b>high address: </b>0x#{current.highAddr.toString(16)}</h5>")
            ancIdElems.append txt
         if !current.isRegset
            txt = $('<h5 id="regwidth" data-width="' + current.width + '"><b>width: </b>' + current.width + '</h5>')
            ancIdElems.append txt   
         #
         # ---- create path w/ divs and table of replicated ancestors
         path = ''
         numReps = 0
         anctable = $('<table class="table">')
         hrow = $("<tr>")
         hrow.addClass("active")
         hrow.append $("<th>name</th>")
         hrow.append $("<th>reps</th>")
         hrow.append $("<th>stride (0x)</th>")
         hrow.append $("<th>idx</th>")
         hdr = $("<thead>").append hrow
         anctable.append hdr 
         bod = $("<tbody>")
         anctable.append bod    
         # ----  classes for addresscalc: ancrepinput (input tag reps for each ancestor w/ maxrep/stride info)
         #             , ancreps (number in path). id for ancaddr (calculated address)
         $.each registers, (index, register) ->
            if index > 0 then prefix = '.' else prefix = ''
            suffix = if register.reps > 1 then ('[<span class="ancreps bg-success">0</span>]') else '' 
            path += prefix + register.name + suffix
            if register.reps > 1
               numReps = numReps + 1
               brow = $('<tr>')
               brow.append $("<td>#{register.name}</td>")
               brow.append $("<td>#{register.reps}</td>")
               brow.append $("<td>#{register.stride.toString(16)}</td>")
               repinputEl = $('<input class="ancrepinput" data-reps="' + register.reps + '" data-stride="' + register.stride + '"type="number" min="0" max="' + (register.reps - 1) + '" value="0">')
               tdEl = $('<td>').append repinputEl
               brow.append tdEl 
               bod.append brow
               # ---- attach on-change function to ancestor table elements (rep index inputs)
               repinputEl.change ->
                  # ---- get the calculated address element and base address          
                  ancaddrEl = $("#ancaddr").first()
                  newaddr = ancaddrEl.data('base')
                  # ---- for each anc w reps, update path and newaddr
                  $(".ancrepinput").each (ancidx, anc) ->
                     #alert('index=' + ancidx)
                     maxreps = anc.getAttribute('data-reps')
                     stride = anc.getAttribute('data-stride')
                     inVal = anc.value
                     if (inVal == '') or ((+ inVal) < 0) then inVal = 0
                     if (+ inVal) >= (+ maxreps) then inVal = maxreps - 1
                     #alert($(".ancreps").get(ancidx).innerHTML)
                     $(".ancreps").get(ancidx).textContent = inVal.toString()
                     # alert('index=' + ancidx + ', maxreps=' + maxreps + ', stride=' + stride + ', val=' + inVal)
                     newaddr = newaddr + (inVal * stride)
                  # alert('ancrepinput=' + $(".ancrepinput").size() + ', ancaddr=' + $("#ancaddr").size() + ', ancreps=' + $(".ancreps").size())
                  # ---- change the calculated address text
                  ancaddrEl.text('0x' + newaddr.toString(16))
         # ---- display table and address path if reps found         
         if numReps > 0
            tabcol = $('<div class="col-sm-5 col-sm-offset-1">').append anctable
            tabrow = $('<div class="row">').append tabcol
            ancIdElems.append tabrow        
            pathcol = $('<div class="col-sm-8 col-sm-offset-1">')
            pathrow = $('<div class="row">').append pathcol
            pathcol.append $('<h5><b>' + path + ' : </b><span id="ancaddr" class="bg-success" data-base="' + current.baseAddr + '">0x' + current.baseAddr.toString(16) + '</span></h5>')
            ancIdElems.append pathrow 
         #
         # ---- finish with category, reset if a register, long description
         if (current.catCode > 0)
            txt = $("<h5><b>category: </b>#{createCatString(current.catCode)}</h5>")
            ancIdElems.append txt
         if current.longText?
            txt = $('<p class="bg-info">' + current.longText + '</p>')
            ancIdElems.append txt
         #alert('ancrepinput=' + $(".ancrepinput").size() + ', ancaddr=' + $("#ancaddr").size() + ', ancreps=' + $(".ancreps").size())
      else   
         hdr = $("<h3>Showing root elements</h3>")
         ancIdElems.append hdr         
      
